//
// Created by Davide  Caroselli on 23/08/16.
//

#include "Model.h"
#include "DiagonalAlignment.h"
#include "Corpus.h"

using namespace fastalign;

Model::Model(const bool is_reverse, const bool use_null, const bool favor_diagonal, const double prob_align_null,
             double diagonal_tension) : is_reverse(is_reverse), use_null(use_null), favor_diagonal(favor_diagonal),
                                        prob_align_null(prob_align_null), diagonal_tension(diagonal_tension) {
}

void Model::ComputeAlignments(vector<pair<string, string>> batch, bool updateModel,
                               AlignmentStats *outStats, vector<alignment> *outAlignments) {
    double emp_feat_ = 0.0;
    double c0_ = 0.0;
    double likelihood_ = 0.0;

    if (outAlignments)
        outAlignments->resize(batch.size());

#pragma omp parallel for schedule(dynamic) reduction(+:emp_feat_,c0_,likelihood_)
    for (int line_idx = 0; line_idx < static_cast<int> (batch.size()); ++line_idx) {
        sentence src, trg;

        Corpus::ParseLine(is_reverse ? batch[line_idx].second : batch[line_idx].first, src);
        Corpus::ParseLine(is_reverse ? batch[line_idx].first : batch[line_idx].second, trg);

        vector<double> probs(src.size() + 1);
        alignment outAlignment;

        for (size_t j = 0; j < trg.size(); ++j) {
            const unsigned &f_j = trg[j];
            double sum = 0;
            double prob_a_i = 1.0 / (src.size() +
                                     // uniform (model 1), Diagonal Alignment (distortion model)
                                     // ****** DIFFERENT FROM LEXICAL TRANSLATION PROBABILITY *****
                                     (use_null ? 1 : 0));
            if (use_null) {
                if (favor_diagonal)
                    prob_a_i = prob_align_null;
                probs[0] = s2t.safe_prob(kNullWord, f_j) * prob_a_i;
                sum += probs[0];
            }

            double az = 0;
            if (favor_diagonal)
                az = DiagonalAlignment::ComputeZ(j + 1, trg.size(), src.size(), diagonal_tension) /
                     (1. - prob_align_null);

            for (size_t i = 1; i <= src.size(); ++i) {
                if (favor_diagonal) {
                    prob_a_i = DiagonalAlignment::UnnormalizedProb(j + 1, i, trg.size(), src.size(), diagonal_tension) /
                               az;
                }
                probs[i] = s2t.safe_prob(src[i - 1], f_j) * prob_a_i;
                sum += probs[i];
            }


            if (use_null) {
                double count = probs[0] / sum;
                c0_ += count;

                if (updateModel)
                    s2t.Update(kNullWord, f_j, count);
            }

            if (updateModel || outStats) {
                for (size_t i = 1; i <= src.size(); ++i) {
                    const double p = probs[i] / sum;

                    if (updateModel) {
                        s2t.Update(src[i - 1], f_j, p);
                    }

                    if (outStats)
                        emp_feat_ += DiagonalAlignment::Feature(j, i, trg.size(), src.size()) * p;
                }
            }


            if (outAlignments) {
                double max_p = -1;
                int max_index = -1;
                if (use_null) {
                    max_index = 0;
                    max_p = probs[0];
                }

                for (unsigned i = 1; i <= src.size(); ++i) {
                    if (probs[i] > max_p) {
                        max_index = i;
                        max_p = probs[i];
                    }
                }

                if (max_index > 0) {
                    if (is_reverse)
                        outAlignment.push_back(pair<word, word>(j, max_index - 1));
                    else
                        outAlignment.push_back(pair<word, word>(max_index - 1, j));
                }
            }


            if (outStats)
                likelihood_ += log(sum);
        }

        if (outAlignments)
            (*outAlignments)[line_idx] = outAlignment;
    }

    if (outStats) {
        outStats->emp_feat += emp_feat_;
        outStats->c0 += c0_;
        outStats->likelihood += likelihood_;
    }
}



