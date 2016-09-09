//
// Created by Davide  Caroselli on 23/08/16.
//

#include <iostream>
#include <sstream>
#include <thread>
#include <omp.h>
#include "DiagonalAlignment.h"
#include "ModelBuilder.h"

using namespace mmt;
using namespace mmt::fastalign;

struct LengthPairHash {
    size_t operator()(const pair<length_t, length_t> &x) const {
        return (size_t) ((x.first << 16) | ((x.second) & 0xffff));
    }
};

ModelBuilder::ModelBuilder(Options options) : mean_srclen_multiplier(options.mean_srclen_multiplier),
                                              is_reverse(options.is_reverse),
                                              iterations(options.iterations),
                                              favor_diagonal(options.favor_diagonal),
                                              prob_align_null(options.prob_align_null),
                                              optimize_tension(options.optimize_tension),
                                              variational_bayes(options.variational_bayes),
                                              alpha(options.alpha),
                                              use_null(options.use_null),
                                              buffer_size(options.buffer_size),
                                              threads((options.threads == 0) ? (int) thread::hardware_concurrency()
                                                                             : options.threads) {
    if (variational_bayes && alpha <= 0.0)
        throw invalid_argument("Parameter 'alpha' must be greather than 0");

    model = new Model(is_reverse, use_null, favor_diagonal, prob_align_null, options.initial_diagonal_tension);
}

void ModelBuilder::setListener(ModelBuilder::Listener *listener) {
    ModelBuilder::listener = listener;
}

void
ModelBuilder::AllocateTTableSpace(ttable_t &table, const unordered_map<wid_t, vector<wid_t>> &values,
                                  const wid_t sourceWordMaxValue) {
    if (table.size() <= sourceWordMaxValue)
        table.resize(sourceWordMaxValue + 1);

#pragma omp parallel for schedule(dynamic)
    for (size_t bucket = 0; bucket < values.bucket_count(); ++bucket) {
        for (auto row_ptr = values.begin(bucket); row_ptr != values.end(bucket); ++row_ptr) {
            wid_t sourceWord = row_ptr->first;

            for (auto targetWord = row_ptr->second.begin(); targetWord != row_ptr->second.end(); ++targetWord)
                table[sourceWord][*targetWord] = 0;
        }
    }
}

void ModelBuilder::InitialPass(const Corpus &corpus, double *n_target_tokens, ttable_t &ttable,
                               vector<pair<pair<length_t, length_t>, size_t>> *size_counts) {
    CorpusReader reader(corpus);

    unordered_map<pair<length_t, length_t>, size_t, LengthPairHash> size_counts_;

    unordered_map<wid_t, vector<wid_t>> buffer;
    wid_t maxSourceWord = 0;
    size_t buffer_items = 0;
    vector<wid_t> src, trg;

    while (reader.Read(src, trg)) {
        if (is_reverse)
            swap(src, trg);

        *n_target_tokens += trg.size();

        if (use_null) {
            for (size_t idxf = 0; idxf < trg.size(); ++idxf) {
                buffer[kAlignerNullWord].push_back(trg[idxf]);
            }

            buffer_items += trg.size();
        }

        for (size_t idxe = 0; idxe < src.size(); ++idxe) {
            for (size_t idxf = 0; idxf < trg.size(); ++idxf) {
                maxSourceWord = max(maxSourceWord, src[idxe]);
                buffer[src[idxe]].push_back(trg[idxf]);
            }
            buffer_items += trg.size();
        }

        if (buffer_items > buffer_size * 100) {
            AllocateTTableSpace(ttable, buffer, maxSourceWord);
            buffer_items = 0;
            maxSourceWord = 0;
            buffer.clear();
        }

        ++size_counts_[make_pair<length_t, length_t>((length_t) trg.size(), (length_t) src.size())];
    }

    for (auto p = size_counts_.begin(); p != size_counts_.end(); ++p) {
        size_counts->push_back(*p);
    }

    AllocateTTableSpace(ttable, buffer, maxSourceWord);
}

void ModelBuilder::SwapTTables(ttable_t &source, ttable_t &destination) {
    if (destination.empty()) {
        destination.resize(source.size());
        for (size_t i = 0; i < source.size(); ++i) {
            destination[i] = source[i];
        }
    } else {
        destination.swap(source);
    }
}

void ModelBuilder::ClearTTable(ttable_t &table) {
#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < table.size(); ++i) {
        for (auto cell = table[i].begin(); cell != table[i].end(); ++cell)
            cell->second = 0;
    }
}

Model *ModelBuilder::Build(const Corpus &corpus, const string &model_filename) {
    omp_set_dynamic(0);
    omp_set_num_threads(threads);

    if (listener) listener->Begin();

    vector<pair<pair<length_t, length_t>, size_t>> size_counts;
    double n_target_tokens = 0;

    ttable_t stagingArea;

    if (listener) listener->Begin(kBuilderStepSetup, 0);
    InitialPass(corpus, &n_target_tokens, stagingArea, &size_counts);
    if (listener) listener->End(kBuilderStepSetup, 0);

    for (int iter = 0; iter < iterations; ++iter) {
        if (listener) listener->IterationBegin(iter + 1);

        double emp_feat = 0.0;

        CorpusReader reader(corpus);
        vector<pair<vector<wid_t>, vector<wid_t>>> batch;

        if (listener) listener->Begin(kBuilderStepAligning, iter + 1);
        while (reader.Read(batch, buffer_size)) {
            emp_feat += model->ComputeAlignments(batch, &stagingArea, NULL);
            batch.clear();
        }
        if (listener) listener->End(kBuilderStepAligning, iter + 1);

        emp_feat /= n_target_tokens;

        if (favor_diagonal && optimize_tension) {
            if (listener) listener->Begin(kBuilderStepOptimizingDiagonalTension, iter + 1);

            for (int ii = 0; ii < 8; ++ii) {
                double mod_feat = 0;
#pragma omp parallel for reduction(+:mod_feat)
                for (size_t i = 0; i < size_counts.size(); ++i) {
                    const pair<length_t, length_t> &p = size_counts[i].first;
                    for (length_t j = 1; j <= p.first; ++j)
                        mod_feat += size_counts[i].second *
                                    DiagonalAlignment::ComputeDLogZ(j, p.first, p.second, model->diagonal_tension);
                }
                mod_feat /= n_target_tokens;
                model->diagonal_tension += (emp_feat - mod_feat) * 20.0;
                if (model->diagonal_tension <= 0.1) model->diagonal_tension = 0.1;
                if (model->diagonal_tension > 14) model->diagonal_tension = 14;
            }

            if (listener) listener->End(kBuilderStepOptimizingDiagonalTension, iter + 1);
        }

        if (listener) listener->Begin(kBuilderStepNormalizing, iter + 1);
        NormalizeTTable(stagingArea, variational_bayes ? alpha : 0);
        SwapTTables(stagingArea, model->translation_table);
        ClearTTable(stagingArea);
        if (listener) listener->End(kBuilderStepNormalizing, iter + 1);

        if (listener) listener->IterationEnd(iter + 1);
    }

    if (listener) listener->Begin(kBuilderStepPruning, 0);
    model->Prune();
    if (listener) listener->End(kBuilderStepPruning, 0);

    if (listener) listener->Begin(kBuilderStepStoringModel, 0);
    model->Store(model_filename);
    if (listener) listener->End(kBuilderStepStoringModel, 0);

    if (listener) listener->End();

    return model;
}

inline double digamma(double x) {
    double result = 0, xx, xx2, xx4;
    for (; x < 7; ++x)
        result -= 1 / x;
    x -= 1.0 / 2.0;
    xx = 1.0 / x;
    xx2 = xx * xx;
    xx4 = xx2 * xx2;
    result += log(x) + (1. / 24.) * xx2 - (7.0 / 960.0) * xx4 + (31.0 / 8064.0) * xx4 * xx2 -
              (127.0 / 30720.0) * xx4 * xx4;
    return result;
}

void ModelBuilder::NormalizeTTable(ttable_t &table, double alpha) {
#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < table.size(); ++i) {
        unordered_map<wid_t, double> &row = table[i];
        double row_norm = 0;

        for (auto cell = row.begin(); cell != row.end(); ++cell)
            row_norm += cell->second + alpha;

        if (row_norm == 0) row_norm = 1;

        if (alpha > 0)
            row_norm = digamma(row_norm);

        for (auto cell = row.begin(); cell != row.end(); ++cell)
            cell->second = alpha > 0 ? exp(digamma(cell->second + alpha) - row_norm) : cell->second / row_norm;
    }
}
