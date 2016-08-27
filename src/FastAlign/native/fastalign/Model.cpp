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

void Model::ComputeAlignments(const vector<pair<string, string>> &batch, ttable_t *outTable,
                              AlignmentStats *outStats, vector<alignment> *outAlignments) {
    vector<pair<sentence, sentence>> parsed_batch;
    parsed_batch.resize(batch.size());

#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < batch.size(); ++i) {
        Corpus::ParseLine(batch[i].first, parsed_batch[i].first);
        Corpus::ParseLine(batch[i].second, parsed_batch[i].second);
    }

    ComputeAlignments(parsed_batch, outTable, outStats, outAlignments);
}

void
Model::ComputeAlignments(const vector<pair<sentence, sentence>> &batch, ttable_t *outTable, AlignmentStats *outStats,
                         vector<alignment> *outAlignments) {
    double emp_feat_ = 0.0;
    double c0_ = 0.0;
    double likelihood_ = 0.0;

    if (outAlignments)
        outAlignments->resize(batch.size());

#pragma omp parallel for schedule(dynamic) reduction(+:emp_feat_,c0_,likelihood_)
    for (size_t line_idx = 0; line_idx < batch.size(); ++line_idx) {
        sentence src = is_reverse ? batch[line_idx].second : batch[line_idx].first;
        sentence trg = is_reverse ? batch[line_idx].first : batch[line_idx].second;

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
                probs[0] = GetProbability(kNullWord, f_j) * prob_a_i;
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
                probs[i] = GetProbability(src[i - 1], f_j) * prob_a_i;
                sum += probs[i];
            }


            if (use_null) {
                double count = probs[0] / sum;
                c0_ += count;

                if (outTable) {
#pragma omp atomic
                    (*outTable)[kNullWord][f_j] += count;
                }
            }

            if (outTable || outStats) {
                for (size_t i = 1; i <= src.size(); ++i) {
                    const double p = probs[i] / sum;

                    if (outTable) {
#pragma omp atomic
                        (*outTable)[src[i - 1]][f_j] += p;
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

void Model::Prune(double threshold) {
#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < translation_table.size(); ++i) {
        unordered_map<word, double> &row = translation_table[i];

        for (auto cell = row.cbegin(); cell != row.cend(); /* no increment */) {
            if (cell->second <= threshold) {
                row.erase(cell++);
            } else {
                ++cell;
            }
        }
    }
}

void Model::Store(const string &filename) {
    ofstream out(filename, ios::binary | ios::out);

    out.write((const char *) &is_reverse, sizeof(bool));
    out.write((const char *) &use_null, sizeof(bool));
    out.write((const char *) &favor_diagonal, sizeof(bool));

    out.write((const char *) &prob_align_null, sizeof(double));
    out.write((const char *) &diagonal_tension, sizeof(double));

    size_t ttable_size = translation_table.size();
    out.write((const char *) &ttable_size, sizeof(size_t));

    for (word sourceWord = 0; sourceWord < translation_table.size(); ++sourceWord) {
        unordered_map<word, double> &row = translation_table[sourceWord];
        size_t row_size = row.size();

        if (row_size == 0)
            continue;

        out.write((const char *) &sourceWord, sizeof(word));
        out.write((const char *) &row_size, sizeof(size_t));

        for (auto it = row.begin(); it != row.end(); ++it) {
            word targetWord = it->first;
            double value = it->second;

            out.write((const char *) &targetWord, sizeof(word));
            out.write((const char *) &value, sizeof(double));
        }
    }
}

Model *Model::Open(const string &filename) {
    bool is_reverse;
    bool use_null;
    bool favor_diagonal;
    double prob_align_null;
    double diagonal_tension;

    ifstream in(filename, ios::binary | ios::in);

    in.read((char *) &is_reverse, sizeof(bool));
    in.read((char *) &use_null, sizeof(bool));
    in.read((char *) &favor_diagonal, sizeof(bool));

    in.read((char *) &prob_align_null, sizeof(double));
    in.read((char *) &diagonal_tension, sizeof(double));

    Model *model = new Model(is_reverse, use_null, favor_diagonal, prob_align_null, diagonal_tension);

    size_t ttable_size;
    in.read((char *) &ttable_size, sizeof(size_t));

    model->translation_table.resize(ttable_size);

    while (true) {
        word sourceWord;
        in.read((char *) &sourceWord, sizeof(word));

        if (in.eof())
            break;

        size_t row_size;
        in.read((char *) &row_size, sizeof(size_t));

        unordered_map<word, double> &row = model->translation_table[sourceWord];
        row.reserve(row_size);

        for (size_t i = 0; i < row_size; ++i) {
            word targetWord;
            double value;

            in.read((char *) &targetWord, sizeof(word));
            in.read((char *) &value, sizeof(double));

            row[targetWord] = value;
        }
    }

    return model;
}
