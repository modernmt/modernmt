//
// Created by Davide  Caroselli on 23/08/16.
//

#include <iostream>
#include <thread>
#include <assert.h>
#include <unordered_set>
#include <boost/filesystem.hpp>
#include "DiagonalAlignment.h"
#include "Builder.h"
#include "BidirectionalModel.h"

#include <math.h>       /* isnormal */

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
    vector<unordered_map<word_t, pair<double, double>>> data;

    BuilderModel(bool is_reverse, bool use_null, bool favor_diagonal, double prob_align_null, double diagonal_tension)
            : Model(is_reverse, use_null, favor_diagonal, prob_align_null, diagonal_tension) {
    }

    ~BuilderModel() {};

    double GetProbability(word_t source, word_t target) override {
        if (data.empty())
            return kNullProbability;
        if (source >= data.size())
            return kNullProbability;

        unordered_map<word_t, pair<double, double>> &row = data[source];
        auto ptr = row.find(target);
        return ptr == row.end() ? kNullProbability : ptr->second.first;
    }

    void IncrementProbability(word_t source, word_t target, double amount) override {
#pragma omp atomic
        data[source][target].second += amount;
    }

    void Prune(double threshold = 1e-20) {
#pragma omp parallel for schedule(dynamic)
        for (size_t i = 0; i < data.size(); ++i) {
            unordered_map<word_t, pair<double, double>> &row = data[i];

            for (auto cell = row.cbegin(); cell != row.cend(); /* no increment */) {
                if (cell->second.first <= threshold)
                    cell = row.erase(cell);
                else
                    ++cell;
            }
        }
    }

    void Normalize(double alpha = 0) {
        for (size_t i = 0; i < data.size(); ++i) {
            unordered_map<word_t, pair<double, double>> &row = data[i];
            double row_norm = 0;

            for (auto cell = row.begin(); cell != row.end(); ++cell)
                row_norm += cell->second.first + alpha;

            if (row_norm == 0) row_norm = 1;

            if (alpha > 0)
                row_norm = digamma(row_norm);

            assert(isnormal(row_norm));

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

    void Store(const string &filename) {
        ofstream out(filename, ios::binary | ios::out);

        out.write((const char *) &use_null, sizeof(bool));
        out.write((const char *) &favor_diagonal, sizeof(bool));

        out.write((const char *) &prob_align_null, sizeof(double));
        out.write((const char *) &diagonal_tension, sizeof(double));

        size_t data_size = data.size();
        out.write((const char *) &data_size, sizeof(size_t));

        for (word_t sourceWord = 0; sourceWord < data_size; ++sourceWord) {
            unordered_map<word_t, pair<double, double>> &row = data[sourceWord];
            size_t row_size = row.size();

            if (!row.empty()) {
                out.write((const char *) &sourceWord, sizeof(word_t));
                out.write((const char *) &row_size, sizeof(size_t));

                for (auto entry = row.begin(); entry != row.end(); ++entry) {
                    float value = (float) (entry->second.first);

                    out.write((const char *) &entry->first, sizeof(word_t));
                    out.write((const char *) &value, sizeof(float));
                }
            }
        }
    }
};

Builder::Builder(Options options) : initial_diagonal_tension(options.initial_diagonal_tension),
                                    iterations(options.iterations),
                                    favor_diagonal(options.favor_diagonal),
                                    prob_align_null(options.prob_align_null),
                                    optimize_tension(options.optimize_tension),
                                    variational_bayes(options.variational_bayes),
                                    alpha(options.alpha),
                                    use_null(options.use_null),
                                    buffer_size(options.buffer_size),
                                    pruning(options.pruning_threshold),
                                    max_length(options.max_line_length),
                                    vocabulary_threshold(options.vocabulary_threshold),
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

void Builder::AllocateTTableSpace(Model *_model, const unordered_map<word_t, wordvec_t> &values,
                                  const word_t sourceWordMaxValue) {
    BuilderModel *model = (BuilderModel *) _model;
    if (model->data.size() <= sourceWordMaxValue)
        model->data.resize(sourceWordMaxValue + 1);

#pragma omp parallel for schedule(dynamic)
    for (size_t bucket = 0; bucket < values.bucket_count(); ++bucket) {
        for (auto row_ptr = values.begin(bucket); row_ptr != values.end(bucket); ++row_ptr) {
            word_t sourceWord = row_ptr->first;

            for (auto targetWord = row_ptr->second.begin(); targetWord != row_ptr->second.end(); ++targetWord)
                model->data[sourceWord][*targetWord] = pair<double, double>(kNullProbability, 0);
        }
    }
}

void Builder::InitialPass(const Vocabulary *vocab, Model *_model, const Corpus &corpus, double *n_target_tokens,
                          vector<pair<pair<length_t, length_t>, size_t>> *size_counts) {
    BuilderModel *model = (BuilderModel *) _model;
    CorpusReader reader(corpus, vocab, max_length, true);

    unordered_map<pair<length_t, length_t>, size_t, LengthPairHash> size_counts_;

    unordered_map<word_t, wordvec_t> buffer;
    word_t maxSourceWord = 0;
    size_t buffer_items = 0;
    wordvec_t src, trg;

    while (reader.Read(src, trg)) {
        if (model->is_reverse)
            swap(src, trg);

        *n_target_tokens += trg.size();

        if (use_null) {
            for (size_t idxf = 0; idxf < trg.size(); ++idxf) {
                buffer[kNullWord].push_back(trg[idxf]);
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

void Builder::Build(const Corpus &corpus, const string &path) {
    CorpusReader reader(corpus, nullptr, max_length, true);

    if (listener) listener->VocabularyBuildBegin();
    const Vocabulary *vocab = Vocabulary::FromCorpus(reader, vocabulary_threshold);
    if (listener) listener->VocabularyBuildEnd();

    BuilderModel *forward = (BuilderModel *) BuildModel(vocab, corpus, true);
    fs::path fwd_model_filename = fs::absolute(fs::path(path) / fs::path("fwd_model.tmp"));
    forward->Store(fwd_model_filename.string());
    delete forward;

    BuilderModel *backward = (BuilderModel *) BuildModel(vocab, corpus, false);
    fs::path bwd_model_filename = fs::absolute(fs::path(path) / fs::path("bwd_model.tmp"));
    backward->Store(bwd_model_filename.string());
    delete backward;

    if (listener) listener->ModelDumpBegin();
    fs::path model_filename = fs::absolute(fs::path(path) / fs::path("model.dat"));
    MergeAndStore(fwd_model_filename.string(), bwd_model_filename.string(), model_filename.string());

    fs::path vocab_filename = fs::absolute(fs::path(path) / fs::path("model.voc"));
    vocab->Store(vocab_filename.string());
    delete vocab;

    if (remove(fwd_model_filename.c_str()) != 0)
        throw runtime_error("Error deleting the forward model file");

    if (remove(bwd_model_filename.c_str()) != 0)
        throw runtime_error("Error deleting the backward model file");

    if (listener) listener->ModelDumpEnd();
}

Model *Builder::BuildModel(const Vocabulary *vocab, const Corpus &corpus, bool forward) {
    BuilderModel *model = new BuilderModel(!forward, use_null, favor_diagonal, prob_align_null,
                                           initial_diagonal_tension);

    if (listener) listener->Begin(forward);

    vector<pair<pair<length_t, length_t>, size_t>> size_counts;
    double n_target_tokens = 0;

    if (listener) listener->Begin(forward, kBuilderStepSetup, 0);
    InitialPass(vocab, model, corpus, &n_target_tokens, &size_counts);
    if (listener) listener->End(forward, kBuilderStepSetup, 0);

    for (int iter = 0; iter < iterations; ++iter) {
        if (listener) listener->IterationBegin(forward, iter + 1);

        double emp_feat = 0.0;

        CorpusReader reader(corpus, vocab, max_length, true);

        vector<pair<wordvec_t, wordvec_t>> batch;

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
                    double _tmp_mod_feat = 0.0;
                    for (length_t j = 1; j <= p.first; ++j)
                        _tmp_mod_feat += DiagonalAlignment::ComputeDLogZ(j, p.first, p.second, model->diagonal_tension);
                    mod_feat += size_counts[i].second * _tmp_mod_feat;
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
    model->Prune(pruning);
    if (listener) listener->End(forward, kBuilderStepPruning, 0);

    if (listener) listener->End(forward);

    return model;
}

void Builder::MergeAndStore(const string &fwd_path, const string &bwd_path, const string &out_path) {
    //creating the bitable
    bitable_t *table = new bitable_t;

    //opening forward model file for reading
    ifstream fwd_in(fwd_path, ios::binary | ios::in);

    //loading forward header
    bool fwd_use_null, fwd_favor_diagonal;
    double fwd_prob_align_null, fwd_diagonal_tension;
    size_t fwd_ttable_size;

    fwd_in.read((char *) &fwd_use_null, sizeof(bool));
    fwd_in.read((char *) &fwd_favor_diagonal, sizeof(bool));
    fwd_in.read((char *) &fwd_prob_align_null, sizeof(double));
    fwd_in.read((char *) &fwd_diagonal_tension, sizeof(double));
    fwd_in.read((char *) &fwd_ttable_size, sizeof(size_t));

    if (fwd_ttable_size == 0)
        throw runtime_error("The forward model is empty");

    //resizing the bitable
    table->resize(fwd_ttable_size);

    //loading forward entries and fill the bitable
    word_t sourceWord, targetWord;
    size_t rowSize;
    float score;

    while (true) {
        fwd_in.read((char *) &sourceWord, sizeof(word_t));
        if (fwd_in.eof())
            break;

        fwd_in.read((char *) &rowSize, sizeof(size_t));
        table->at(sourceWord).reserve(rowSize);
        for (size_t i = 0; i < rowSize; ++i) {
            fwd_in.read((char *) &targetWord, sizeof(word_t));
            fwd_in.read((char *) &score, sizeof(float));
            table->at(sourceWord)[targetWord] = pair<float, float>(score, kNullProbability);
        }
    }

    //closing forward model file
    fwd_in.close();

    //opening backward model file for reading
    ifstream bwd_in(bwd_path, ios::binary | ios::in);

    //loading backward header
    bool bwd_use_null, bwd_favor_diagonal;
    double bwd_prob_align_null, bwd_diagonal_tension;
    size_t bwd_ttable_size;

    bwd_in.read((char *) &bwd_use_null, sizeof(bool));
    bwd_in.read((char *) &bwd_favor_diagonal, sizeof(bool));
    bwd_in.read((char *) &bwd_prob_align_null, sizeof(double));
    bwd_in.read((char *) &bwd_diagonal_tension, sizeof(double));
    bwd_in.read((char *) &bwd_ttable_size, sizeof(size_t));

    //checking consistency of forward and backward models
    assert(fwd_use_null == bwd_use_null);
    assert(fwd_favor_diagonal == bwd_favor_diagonal);
    assert(fwd_prob_align_null == bwd_prob_align_null);
    assert(fwd_ttable_size == bwd_ttable_size);

    if (bwd_ttable_size == 0)
        throw runtime_error("The backward model is empty");

    //loading backward entries and fill the bitable
    while (true) {
        bwd_in.read((char *) &targetWord, sizeof(word_t));
        if (bwd_in.eof())
            break;

        bwd_in.read((char *) &rowSize, sizeof(size_t));
        for (size_t i = 0; i < rowSize; ++i) {
            bwd_in.read((char *) &sourceWord, sizeof(word_t));
            bwd_in.read((char *) &score, sizeof(float));

            assert(sourceWord < table->size());

            auto cell = table->at(sourceWord).emplace(targetWord,
                                                      pair<float, float>(kNullProbability, kNullProbability));
            pair<float, float> &el = cell.first->second;
            el.second = score;
        }

    }

    //closing backward model file
    bwd_in.close();

    //opening bidirectional model file for writing
    ofstream out(out_path, ios::binary | ios::out);

    //writing header
    out.write((const char *) &fwd_use_null, sizeof(bool));
    out.write((const char *) &fwd_favor_diagonal, sizeof(bool));
    out.write((const char *) &fwd_prob_align_null, sizeof(double));
    out.write((const char *) &fwd_diagonal_tension, sizeof(double));
    out.write((const char *) &bwd_diagonal_tension, sizeof(double));
    out.write((const char *) &fwd_ttable_size, sizeof(size_t));

    //writing all entries of the bitable
    for (word_t sourceWord = 0; sourceWord < fwd_ttable_size; ++sourceWord) {
        auto &row = table->at(sourceWord);

        rowSize = row.size();
        out.write((const char *) &sourceWord, sizeof(word_t));
        out.write((const char *) &rowSize, sizeof(size_t));
        for (auto trgEntry = row.begin(); trgEntry != row.end(); ++trgEntry) {
            out.write((const char *) &trgEntry->first, sizeof(word_t));
            out.write((const char *) &trgEntry->second.first, sizeof(float));
            out.write((const char *) &trgEntry->second.second, sizeof(float));
        }
    }
    //closing bidirectional model file
    out.close();

    //deleting bitable
    delete table;
}
