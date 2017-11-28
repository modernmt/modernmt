//
// Created by Davide  Caroselli on 23/08/16.
//

#include "Model.h"
#include "Vocabulary.h"
#include "DiagonalAlignment.h"
#include "Corpus.h"

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

Model::Model(bool is_reverse, bool use_null, bool favor_diagonal, double prob_align_null, double diagonal_tension)
        : is_reverse(is_reverse), use_null(use_null), favor_diagonal(favor_diagonal), prob_align_null(prob_align_null),
          diagonal_tension(diagonal_tension) {
}

double Model::ComputeScores(const vector<pair<wordvec_t, wordvec_t>> &batch, Model *outModel,
                                vector<double> *outScores) {
    double current_emp_feat = 0.0, emp_feat = 0.0;

    if (outScores)
        outScores->resize(batch.size());

#pragma omp parallel for schedule(dynamic) reduction(+:emp_feat)
    for (size_t i = 0; i < batch.size(); ++i) {
        const pair<wordvec_t, wordvec_t> &p = batch[i];
        current_emp_feat = ComputeAlignment(p.first, p.second, outModel, NULL);
        emp_feat += current_emp_feat;
        if (outScores)
            outScores->at(i) = (double) current_emp_feat;
    }

    return emp_feat;
}

double Model::ComputeAlignments(const vector<pair<wordvec_t, wordvec_t>> &batch, Model *outModel,
                                vector<alignment_t> *outAlignments) {
    double emp_feat = 0.0;

    if (outAlignments)
        outAlignments->resize(batch.size());

#pragma omp parallel for schedule(dynamic) reduction(+:emp_feat)
    for (size_t i = 0; i < batch.size(); ++i) {
        const pair<wordvec_t, wordvec_t> &p = batch[i];
        emp_feat += ComputeAlignment(p.first, p.second, outModel, outAlignments ? &outAlignments->at(i) : NULL);
    }

    return emp_feat;
}

double Model::ComputeAlignment(const wordvec_t &source, const wordvec_t &target, Model *outModel,
                               alignment_t *outAlignment) {
    double emp_feat = 0.0;

    const wordvec_t src = is_reverse ? target : source;
    const wordvec_t trg = is_reverse ? source : target;

    vector<double> probs(src.size() + 1);

    length_t src_size = (length_t) src.size();
    length_t trg_size = (length_t) trg.size();

    for (length_t j = 0; j < trg_size; ++j) {
        const word_t &f_j = trg[j];
        double sum = 0;
        double prob_a_i = 1.0 / (src_size +
                                 // uniform (model 1), Diagonal Alignment (distortion model)
                                 // ****** DIFFERENT FROM LEXICAL TRANSLATION PROBABILITY *****
                                 (use_null ? 1 : 0));
        if (use_null) {
            if (favor_diagonal)
                prob_a_i = prob_align_null;
            probs[0] = GetProbability(kNullWord, f_j) * prob_a_i;
            sum += probs[0];
        }

        double az = 0;
        if (favor_diagonal)
            az = DiagonalAlignment::ComputeZ(j + 1, trg_size, src_size, diagonal_tension) /
                 (1. - prob_align_null);

        for (length_t i = 1; i <= src_size; ++i) {
            if (favor_diagonal) {
                prob_a_i = DiagonalAlignment::UnnormalizedProb(j + 1, i, trg_size, src_size, diagonal_tension) /
                           az;
            }
            probs[i] = GetProbability(src[i - 1], f_j) * prob_a_i;
            sum += probs[i];
        }


        if (use_null) {
            double count = probs[0] / sum;

            if (outModel)
                outModel->IncrementProbability(kNullWord, f_j, count);
        }

        for (length_t i = 1; i <= src_size; ++i) {
            const double p = probs[i] / sum;

            if (outModel)
                outModel->IncrementProbability(src[i - 1], f_j, p);

            emp_feat += DiagonalAlignment::Feature(j, i, trg_size, src_size) * p;
        }


        if (outAlignment) {
            double max_p = -1;
            int max_index = -1;
            if (use_null) {
                max_index = 0;
                max_p = probs[0];
            }

            for (length_t i = 1; i <= src_size; ++i) {
                if (probs[i] > max_p) {
                    max_index = i;
                    max_p = probs[i];
                }
            }

            if (max_index > 0) {
                if (is_reverse)
                    outAlignment->push_back(pair<length_t, length_t>(j, max_index - 1));
                else
                    outAlignment->push_back(pair<length_t, length_t>(max_index - 1, j));
            }
        }
    }

    return emp_feat;
}