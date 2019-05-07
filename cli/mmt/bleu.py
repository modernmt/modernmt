import argparse
import html

import regex as re
import sacrebleu

__TAG_NAME = r'([a-zA-Z]|_|:)([a-zA-Z]|[0-9]|\.|-|_|:|)*'
_R_TAG = re.compile('(<(' + __TAG_NAME + ')[^>]*/?>)|(<!(' + __TAG_NAME + ')[^>]*[^/]>)|(</(' +
                    __TAG_NAME + ')[^>]*>)|(<!--)|(-->)')
_R_RP = re.compile(r'(\P{N})(\p{P})')
_R_LP = re.compile(r'(\p{P})(\P{N})')
_R_SPLIT = re.compile(r'[\p{InCJK_Unified_Ideographs}\p{S}]')
_R_WS = re.compile(r'\s+')
_R_TAG_PH = re.compile(r'TKXMLTAG[0-9]+')


def _tokenize(text):
    xml_tags = []

    def _collect_xml(match):
        xml_tags.append(match.group())
        return ' TKXMLTAG%d ' % (len(xml_tags) - 1)

    def _restore_xml(match):
        i = int(match.group()[8:])
        return xml_tags[i]

    # Remove XML tags
    text = _R_TAG.sub(_collect_xml, text)

    # De-escape XML entities
    text = html.unescape(text)

    # Split by non-digit punctuation
    text = _R_RP.sub(lambda m: '%s %s ' % (m.group(1), m.group(2)), text)
    text = _R_LP.sub(lambda m: ' %s %s' % (m.group(1), m.group(2)), text)

    # Split every CJK char and symbol
    text = _R_SPLIT.sub(lambda m: ' %s ' % m.group(), text)

    # Restore XML
    text = _R_TAG_PH.sub(_restore_xml, text)

    # Normalize whitespaces
    text = _R_WS.sub(' ', text)
    text = text.strip()

    return text


def sentence_bleu(reference, hypothesis, tokenize=True):
    if tokenize:
        hypothesis = _tokenize(hypothesis)
        reference = _tokenize(reference)

    bleu = sacrebleu.corpus_bleu(hypothesis, reference,
                                 smooth_method='floor', use_effective_order=True, tokenize='none')
    return bleu.score


def corpus_bleu(reference, hypothesis, tokenize=True):
    if tokenize:
        hypothesis = [_tokenize(x) for x in hypothesis]
        reference = [_tokenize(x) for x in reference]

    bleu = sacrebleu.corpus_bleu(hypothesis, [reference], tokenize='none', smooth_method='add-k')
    return bleu.score


def _main():
    parser = argparse.ArgumentParser(description='BLEU score of a file given the reference')
    parser.add_argument('reference', help='the reference file')
    parser.add_argument('hypothesis', help='the hypothesis file')
    parser.add_argument('--raw', dest='tokenize', action='store_false', default=True, help='skip text tokenization')

    args = parser.parse_args()

    with open(args.reference, 'r', encoding='utf-8') as ref, open(args.hypothesis, 'r', encoding='utf-8') as hyp:
        bleu = corpus_bleu(ref, hyp, tokenize=args.tokenize)

    print(bleu)


if __name__ == '__main__':
    _main()
