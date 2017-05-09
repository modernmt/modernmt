//
// Created by Davide  Caroselli on 23/08/16.
//

#include <iostream>
#include <thread>
#include <mmt/aligner/Aligner.h>
#include <boost/filesystem.hpp>
#include "DiagonalAlignment.h"
#include "Builder.h"
#include "BidirectionalModel.h"

#ifdef _OPENMP
#include <omp.h>
#endif

namespace fs = boost::filesystem;

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

struct LengthPairHash {
    size_t operator()(const pair<length_t, length_t> &x) const {
        return (size_t) ((x.first << 16) | ((x.second) & 0xffff));
    }
};

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

class BuilderModel : public Model {
public:
    vector<unordered_map<wid_t, pair<double, double>>> data;

    BuilderModel(bool is_reverse, bool use_null, bool favor_diagonal, double prob_align_null, double diagonal_tension)
            : Model(is_reverse, use_null, favor_diagonal, prob_align_null, diagonal_tension) {
    }

    double GetProbability(wid_t source, wid_t target) override {
        if (data.empty())
            return kNullProbability;
        if (source >= data.size())
            return kNullProbability;

        unordered_map<wid_t, pair<double, double>> &row = data[source];
        auto ptr = row.find(target);
        return ptr == row.end() ? kNullProbability : ptr->second.first;
    }

    void IncrementProbability(wid_t source, wid_t target, double amount) override {
#pragma omp atomic
        data[source][target].second += amount;
    }

    void Prune(double threshold = 1e-20) {
#pragma omp parallel for schedule(dynamic)
        for (size_t i = 0; i < data.size(); ++i) {
            unordered_map<wid_t, pair<double, double>> &row = data[i];

            for (auto cell = row.cbegin(); cell != row.cend(); /* no increment */) {
                if (cell->second.first <= threshold)
                    row.erase(cell++);
                else
                    ++cell;
            }
        }
    }

    void Normalize(double alpha = 0) {
        for (size_t i = 0; i < data.size(); ++i) {
            unordered_map<wid_t, pair<double, double>> &row = data[i];
            double row_norm = 0;

            for (auto cell = row.begin(); cell != row.end(); ++cell)
                row_norm += cell->second.first + alpha;

            if (row_norm == 0) row_norm = 1;

            if (alpha > 0)
                row_norm = digamma(row_norm);

            for (auto cell = row.begin(); cell != row.end(); ++cell)
                cell->second.first =
                        alpha > 0 ?
                        exp(digamma(cell->second.first + alpha) - row_norm) :
                        cell->second.first / row_norm;
        }
    }

    void Swap() {
#pragma omp parallel for schedule(dynamic)
        for (size_t i = 0; i < data.size(); ++i) {
            for (auto cell = data[i].begin(); cell != data[i].end(); ++cell) {
                cell->second.first = cell->second.second;
                cell->second.second = 0;
            }
        }
    }
};

Builder::Builder(Options options) : mean_srclen_multiplier(options.mean_srclen_multiplier),
                                    initial_diagonal_tension(options.initial_diagonal_tension),
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

#ifdef _OPENMP
    omp_set_dynamic(0);
    omp_set_num_threads(threads);
#endif
}

void Builder::setListener(Builder::Listener *listener) {
    Builder::listener = listener;
}

void Builder::AllocateTTableSpace(Model *_model, const unordered_map<wid_t, vector<wid_t>> &values,
                                  const wid_t sourceWordMaxValue) {
    BuilderModel *model = (BuilderModel *) _model;
    if (model->data.size() <= sourceWordMaxValue)
        model->data.resize(sourceWordMaxValue + 1);

#pragma omp parallel for schedule(dynamic)
    for (size_t bucket = 0; bucket < values.bucket_count(); ++bucket) {
        for (auto row_ptr = values.begin(bucket); row_ptr != values.end(bucket); ++row_ptr) {
            wid_t sourceWord = row_ptr->first;

            for (auto targetWord = row_ptr->second.begin(); targetWord != row_ptr->second.end(); ++targetWord)
                model->data[sourceWord][*targetWord] = pair<double, double>(kNullProbability, 0);
        }
    }
}

void Builder::InitialPass(const Corpus &corpus, double *n_target_tokens, Model *_model,
                          vector<pair<pair<length_t, length_t>, size_t>> *size_counts) {
    BuilderModel *model = (BuilderModel *) _model;
    CorpusReader reader(corpus);

    unordered_map<pair<length_t, length_t>, size_t, LengthPairHash> size_counts_;

    unordered_map<wid_t, vector<wid_t>> buffer;
    wid_t maxSourceWord = 0;
    size_t buffer_items = 0;
    vector<wid_t> src, trg;

    while (reader.Read(src, trg)) {
        if (model->is_reverse)
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
            AllocateTTableSpace(model, buffer, maxSourceWord);
            buffer_items = 0;
            maxSourceWord = 0;
            buffer.clear();
        }

        ++size_counts_[make_pair<length_t, length_t>((length_t) trg.size(), (length_t) src.size())];
    }

    for (auto p = size_counts_.begin(); p != size_counts_.end(); ++p) {
        size_counts->push_back(*p);
    }

    AllocateTTableSpace(model, buffer, maxSourceWord);
}

bitable_t *MergeModels(BuilderModel *forward, BuilderModel *backward) {
    bitable_t *table = new bitable_t;
    table->resize(forward->data.size());

    for (wid_t source = 0; source < forward->data.size(); ++source) {
        table->at(source).reserve(forward->data[source].size());

        for (auto entry = forward->data[source].begin(); entry != forward->data[source].end(); ++entry) {
            wid_t target = entry->first;
            double score = entry->second.first;

            table->at(source)[target] = pair<float, float>(score, kNullProbability);
        }
    }

    for (wid_t target = 0; target < backward->data.size(); ++target) {
        for (auto entry = backward->data[target].begin(); entry != backward->data[target].end(); ++entry) {
            wid_t source = entry->first;
            double score = entry->second.first;

            if (table->size() <= source)
                table->resize(source + 1);

            auto cell = table->at(source).emplace(target, pair<float, float>(kNullProbability, kNullProbability));
            pair<float, float> &el = cell.first->second;
            el.second = (float) score;
        }
    }

    return table;
}

void Builder::Build(const Corpus &corpus, const string &path) {
    fs::path filename = fs::absolute(fs::path(path) / fs::path("model.dat"));

    BuilderModel *forward = (BuilderModel *) BuildModel(corpus, true);
    BuilderModel *backward = (BuilderModel *) BuildModel(corpus, false);

    if (listener) listener->ModelDumpBegin();

    shared_ptr<bitable_t> table(MergeModels(forward, backward));
    delete forward;
    delete backward;

    BidirectionalModel _forward(table, true, use_null, favor_diagonal, prob_align_null, forward->diagonal_tension);
    BidirectionalModel _backward(table, false, use_null, favor_diagonal, prob_align_null, backward->diagonal_tension);

    BidirectionalModel::Store(&_forward, &_backward, filename.string());

    if (listener) listener->ModelDumpEnd();
}

Model *Builder::BuildModel(const Corpus &corpus, bool forward) {
    BuilderModel *model = new BuilderModel(!forward, use_null, favor_diagonal, prob_align_null,
                                           initial_diagonal_tension);

    if (listener) listener->Begin(forward);

    vector<pair<pair<length_t, length_t>, size_t>> size_counts;
    double n_target_tokens = 0;

    if (listener) listener->Begin(forward, kBuilderStepSetup, 0);
    InitialPass(corpus, &n_target_tokens, model, &size_counts);
    if (listener) listener->End(forward, kBuilderStepSetup, 0);

    for (int iter = 0; iter < iterations; ++iter) {
        if (listener) listener->IterationBegin(forward, iter + 1);

        double emp_feat = 0.0;

        CorpusReader reader(corpus);
        vector<pair<vector<wid_t>, vector<wid_t>>> batch;

        if (listener) listener->Begin(forward, kBuilderStepAligning, iter + 1);
        while (reader.Read(batch, buffer_size)) {
            emp_feat += model->ComputeAlignments(batch, model, NULL);
            batch.clear();
        }
        if (listener) listener->End(forward, kBuilderStepAligning, iter + 1);

        emp_feat /= n_target_tokens;

        if (favor_diagonal && optimize_tension) {
            if (listener) listener->Begin(forward, kBuilderStepOptimizingDiagonalTension, iter + 1);

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

            if (listener) listener->End(forward, kBuilderStepOptimizingDiagonalTension, iter + 1);
        }

        if (listener) listener->Begin(forward, kBuilderStepNormalizing, iter + 1);
        model->Swap();
        model->Normalize(variational_bayes ? alpha : 0);
        if (listener) listener->End(forward, kBuilderStepNormalizing, iter + 1);

        if (listener) listener->IterationEnd(forward, iter + 1);
    }

    if (listener) listener->Begin(forward, kBuilderStepPruning, 0);
    model->Prune();
    if (listener) listener->End(forward, kBuilderStepPruning, 0);

    if (listener) listener->End(forward);

    return model;
}