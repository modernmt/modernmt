//
// Created by Davide  Caroselli on 23/08/16.
//

#include <mmt/aligner/Aligner.h>
#include "Model.h"
#include "DiagonalAlignment.h"
#include "Corpus.h"

using namespace mmt;
using namespace mmt::fastalign;

Model::Model(const bool is_reverse, const bool use_null, const bool favor_diagonal, const double prob_align_null,
             double diagonal_tension) : is_reverse(is_reverse), use_null(use_null), favor_diagonal(favor_diagonal),
                                        prob_align_null(prob_align_null), diagonal_tension(diagonal_tension) {
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
        wid_t sourceWord;
        in.read((char *) &sourceWord, sizeof(wid_t));

        if (in.eof())
            break;

        size_t row_size;
        in.read((char *) &row_size, sizeof(size_t));

        unordered_map<wid_t, double> &row = model->translation_table[sourceWord];
        row.reserve(row_size);

        for (size_t i = 0; i < row_size; ++i) {
            wid_t targetWord;
            double value;

            in.read((char *) &targetWord, sizeof(wid_t));
            in.read((char *) &value, sizeof(double));

            row[targetWord] = value;
        }
    }

    return model;
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

    for (wid_t sourceWord = 0; sourceWord < translation_table.size(); ++sourceWord) {
        unordered_map<wid_t, double> &row = translation_table[sourceWord];
        size_t row_size = row.size();

        if (row_size == 0)
            continue;

        out.write((const char *) &sourceWord, sizeof(wid_t));
        out.write((const char *) &row_size, sizeof(size_t));

        for (auto it = row.begin(); it != row.end(); ++it) {
            wid_t targetWord = it->first;
            double value = it->second;

            out.write((const char *) &targetWord, sizeof(wid_t));
            out.write((const char *) &value, sizeof(double));
        }
    }
}

void Model::Prune(double threshold) {
#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < translation_table.size(); ++i) {
        unordered_map<wid_t, double> &row = translation_table[i];

        for (auto cell = row.cbegin(); cell != row.cend(); /* no increment */) {
            if (cell->second <= threshold) {
                row.erase(cell++);
            } else {
                ++cell;
            }
        }
    }
}

double Model::ComputeAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch, ttable_t *outTable,
                                vector<alignment_t> *outAlignments) {
    double emp_feat = 0.0;

    if (outAlignments)
        outAlignments->resize(batch.size());

#pragma omp parallel for schedule(dynamic) reduction(+:emp_feat)
    for (size_t i = 0; i < batch.size(); ++i) {
        const pair<vector<wid_t>, vector<wid_t>> &p = batch[i];
        emp_feat += ComputeAlignment(p.first, p.second, outTable, outAlignments ? &outAlignments->at(i) : NULL);
    }

    return emp_feat;
}

double Model::ComputeAlignment(const vector<wid_t> &source, const vector<wid_t> &target, ttable_t *outTable,
                               alignment_t *outAlignment) {
    double emp_feat = 0.0;

    const vector<wid_t> src = is_reverse ? target : source;
    const vector<wid_t> trg = is_reverse ? source : target;

    vector<double> probs(src.size() + 1);

    length_t src_size = (length_t) src.size();
    length_t trg_size = (length_t) trg.size();

    for (length_t j = 0; j < trg_size; ++j) {
        const wid_t &f_j = trg[j];
        double sum = 0;
        double prob_a_i = 1.0 / (src_size +
                                 // uniform (model 1), Diagonal Alignment (distortion model)
                                 // ****** DIFFERENT FROM LEXICAL TRANSLATION PROBABILITY *****
                                 (use_null ? 1 : 0));
        if (use_null) {
            if (favor_diagonal)
                prob_a_i = prob_align_null;
            probs[0] = GetProbability(kAlignerNullWord, f_j) * prob_a_i;
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

            if (outTable) {
#pragma omp atomic
                (*outTable)[kAlignerNullWord][f_j] += count;
            }
        }

        for (length_t i = 1; i <= src_size; ++i) {
            const double p = probs[i] / sum;

            if (outTable) {
#pragma omp atomic
                (*outTable)[src[i - 1]][f_j] += p;
            }

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
                    outAlignment->push_back(pair<wid_t, wid_t>(j, max_index - 1));
                else
                    outAlignment->push_back(pair<wid_t, wid_t>(max_index - 1, j));
            }
        }
    }

    return emp_feat;
}