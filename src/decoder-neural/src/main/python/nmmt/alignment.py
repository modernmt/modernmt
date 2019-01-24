import numpy as np


# - Symmetrization strategies ------------------------------------------------------------------------------------------


def sym_direct(i2o, o2i, ilen, olen):
    return i2o


def sym_inverse(i2o, o2i, ilen, olen):
    return o2i


def sym_intersect(i2o, o2i, ilen, olen):
    return sorted(set(i2o) & set(o2i))


def sym_union(i2o, o2i, ilen, olen):
    return sorted(set(i2o) | set(o2i))


def sym_grow(i2o, o2i, ilen, olen):
    union = sym_union(i2o, o2i, ilen, olen)
    alignment = sym_intersect(i2o, o2i, ilen, olen)

    new_points_added = True
    while new_points_added:
        new_points_added = False
        for i in range(ilen):
            for o in range(olen):
                if (i, o) in alignment:
                    for (i_new, o_new) in _neighboring_points_orthogonal((i, o), ilen, olen):
                        if not (_aligned_o(o_new, ilen, alignment) and _aligned_i(i_new, olen, alignment)) \
                                and ((i_new, o_new) in union):
                            alignment.append((i_new, o_new))
                            new_points_added = True

    return alignment


def sym_grow_diagonal(i2o, o2i, ilen, olen):
    union = sym_union(i2o, o2i, ilen, olen)
    alignment = sym_intersect(i2o, o2i, ilen, olen)

    new_points_added = True
    while new_points_added:
        new_points_added = False
        for i in range(ilen):
            for o in range(olen):
                if (i, o) in alignment:
                    for (i_new, o_new) in _neighboring_points_orthogonal((i, o), ilen, olen):
                        if not (_aligned_o(o_new, ilen, alignment) and _aligned_i(i_new, olen, alignment)) \
                                and ((i_new, o_new) in union):
                            alignment.append((i_new, o_new))
                            new_points_added = True

    while new_points_added:
        new_points_added = False
        for i in range(ilen):
            for o in range(olen):
                if (i, o) in alignment:
                    for (i_new, o_new) in _neighboring_points_diagonal((i, o), ilen, olen):
                        if not (_aligned_o(o_new, ilen, alignment) and _aligned_i(i_new, olen, alignment)) \
                                and ((i_new, o_new) in union):
                            alignment.append((i_new, o_new))
                            new_points_added = True

    return alignment


def sym_grow_diagonal_final_and(i2o, o2i, ilen, olen):
    alignment = sym_grow_diagonal(i2o, o2i, ilen, olen)
    _final(alignment, i2o, o2i, ilen, olen)

    return alignment


# - Make alignment -----------------------------------------------------------------------------------------------------

def make_alignment(source_indexes, target_indexes, attention_matrix, symmetrize=sym_intersect):
    attention_matrix = np.asarray(attention_matrix)

    # resulting shape (layers, batch, heads, output, input);
    # last two dimensions truncated to the size of trg_sub_tokens and src_sub_tokens
    reduced_attention_matrix = attention_matrix[:, :, :, :len(target_indexes), :len(source_indexes)]
    # get average over layers and heads; resulting shape (batch, output, input)
    average_encdec_atts_mats = reduced_attention_matrix.mean((0, 2))
    # get first batch only; resulting shape (output, input)
    alignment_matrix = average_encdec_atts_mats[0]

    s2t_best_indexes = (alignment_matrix / alignment_matrix.sum(axis=0)[np.newaxis:]).argmax(0)
    t2s_best_indexes = (alignment_matrix / alignment_matrix.sum(axis=1)[:, np.newaxis]).argmax(1)

    threshold = 0.8
    s_len = alignment_matrix.shape[1]
    t_len = alignment_matrix.shape[0]

    # select points of the direct alignment (having score >= threshold*best)
    s2t_sub_alignment = []
    for t in range(t_len):
        threshold_value = threshold * alignment_matrix[t, t2s_best_indexes[t]]
        s2t_sub_alignment += [(s, t) for s in range(s_len) if alignment_matrix[t, s] >= threshold_value]

    # select points of the inverted alignment (having score >= threshold*best)
    t2s_sub_alignment = []
    for s in range(s_len):
        threshold_value = threshold * alignment_matrix[s2t_best_indexes[s], s]
        t2s_sub_alignment += [(s, t) for t in range(t_len) if alignment_matrix[t, s] >= threshold_value]

    if not s2t_sub_alignment and not t2s_sub_alignment:
        return []

    # symmetrization on token-based alignment
    s2t_alignment = sorted(set([(source_indexes[al[0]], target_indexes[al[1]]) for al in s2t_sub_alignment]))
    t2s_alignment = sorted(set([(source_indexes[al[0]], target_indexes[al[1]]) for al in t2s_sub_alignment]))
    alignment = sorted(symmetrize(s2t_alignment, t2s_alignment, source_indexes[-1] + 1, target_indexes[-1] + 1))

    return alignment


# - Alignment util functions -------------------------------------------------------------------------------------------

def _neighboring_points_orthogonal((o_index, i_index), e_len, f_len):
    """
    A function that returns list of neighboring points in
    an alignment matrix for a given alignment (pair of indexes)
    """
    result = []

    if o_index > 0:
        result.append((o_index - 1, i_index))
    if i_index > 0:
        result.append((o_index, i_index - 1))
    if o_index < e_len - 1:
        result.append((o_index + 1, i_index))
    if i_index < f_len - 1:
        result.append((o_index, i_index + 1))

    return result


def _neighboring_points_diagonal((o_index, i_index), e_len, f_len):
    """
    A function that returns list of neighboring points in
    an alignment matrix for a given alignment (pair of indexes)
    """
    result = []

    if o_index > 0 and i_index > 0:
        result.append((o_index - 1, i_index - 1))
    if o_index > 0 and i_index < f_len - 1:
        result.append((o_index - 1, i_index + 1))
    if o_index < e_len - 1 and i_index > 0:
        result.append((o_index + 1, i_index - 1))
    if o_index < e_len - 1 and i_index < f_len - 1:
        result.append((o_index + 1, i_index + 1))

    return result


def _neighboring_points((o_index, i_index), e_len, f_len):
    """
    A function that returns list of neighboring points in
    an alignment matrix for a given alignment (pair of indexes)
    """
    result = []

    if o_index > 0:
        result.append((o_index - 1, i_index))
    if i_index > 0:
        result.append((o_index, i_index - 1))
    if o_index < e_len - 1:
        result.append((o_index + 1, i_index))
    if i_index < f_len - 1:
        result.append((o_index, i_index + 1))
    if o_index > 0 and i_index > 0:
        result.append((o_index - 1, i_index - 1))
    if o_index > 0 and i_index < f_len - 1:
        result.append((o_index - 1, i_index + 1))
    if o_index < e_len - 1 and i_index > 0:
        result.append((o_index + 1, i_index - 1))
    if o_index < e_len - 1 and i_index < f_len - 1:
        result.append((o_index + 1, i_index + 1))

    return result


def _aligned_o(o, ilen, alignment):
    """
    A function that checks if a given 'english' word is aligned
    to any foreign word in a given foreign sentence
    """
    for i in range(ilen):
        if (i, o) in alignment:
            return True

    return False


def _aligned_i(i, olen, alignment):
    """
    A function that checks if a given foreign word is aligned
    to any 'english' word in a given 'english' sentence
    """
    # print "i:%s" % (i)
    # print "olen:%s" % (olen)
    # print "len(alignment):%s" % (len(alignment))
    for o in range(olen):
        if (i, o) in alignment:
            return True

    return False


def _final(alignment, i2o, o2i, ilen, olen):
    """
    A function that implements both FINAL(e2f) and FINAL(f2e)
    steps of GROW-DIAG-FINAL algorithm
    """
    for o in range(olen):
        for i in range(ilen):
            if not (aligned_o(o, ilen, alignment) and aligned_i(i, olen, alignment)) \
                    and ((i, o) in i2o or (i, o) in o2i):
                alignment.append((i, o))
