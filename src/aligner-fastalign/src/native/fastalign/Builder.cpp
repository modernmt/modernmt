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
#include "ioutils.h"

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

        io_write(out, use_null);
        io_write(out, favor_diagonal);
        io_write(out, prob_align_null);
        io_write(out, diagonal_tension);

        io_write(out, data.size());

        for (word_t sourceWord = 0; sourceWord < data.size(); ++sourceWord) {
            unordered_map<word_t, pair<double, double>> &row = data[sourceWord];

            if (!row.empty()) {
                io_write(out, sourceWord);
                io_write(out, row.size());

                for (auto entry = row.begin(); entry != row.end(); ++entry) {
                    io_write(out, entry->first);
                    io_write(out, (float) (entry->second.first));
                }
            }
        }
    }
};

Builder::Builder(Options options) : case_sensitive(options.case_sensitive),
                                    initial_diagonal_tension(options.initial_diagonal_tension),
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

void Builder::InitialPass(const Vocabulary &vocab, Model *_model, const std::vector<Corpus> &corpora,
                          double *n_target_tokens, vector<pair<pair<length_t, length_t>, size_t>> *size_counts) {
    auto *model = (BuilderModel *) _model;

    unordered_map<pair<length_t, length_t>, size_t, LengthPairHash> size_counts_;

    unordered_map<word_t, wordvec_t> buffer;
    word_t maxSourceWord = 0;
    size_t buffer_items = 0;
    wordvec_t src, trg;

    for (auto corpus = corpora.begin(); corpus != corpora.end(); ++corpus) {
        CorpusReader reader(*corpus, &vocab, max_length, true);

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
    }

    for (auto p = size_counts_.begin(); p != size_counts_.end(); ++p) {
        size_counts->push_back(*p);
    }

    AllocateTTableSpace(model, buffer, maxSourceWord);
}

void Builder::Build(const std::vector<Corpus> &corpora, const string &path) {
    if (listener) {
        std::ostringstream opts;
        opts << "{"
             << "alpha=" << alpha << ", "
             << "buffer_size=" << buffer_size << ", "
             << "case_sensitive=" << (case_sensitive ? "true" : "false") << ", "
             << "favor_diagonal=" << (favor_diagonal ? "true" : "false") << ", "
             << "initial_diagonal_tension=" << initial_diagonal_tension << ", "
             << "iterations=" << iterations << ", "
             << "max_length=" << max_length << ", "
             << "optimize_tension=" << (optimize_tension ? "true" : "false") << ", "
             << "prob_align_null=" << prob_align_null << ", "
             << "pruning=" << pruning << ", "
             << "threads=" << threads << ", "
             << "use_null=" << (use_null ? "true" : "false") << ", "
             << "variational_bayes=" << (variational_bayes ? "true" : "false") << ", "
             << "vocabulary_threshold=" << vocabulary_threshold
             << "}";

        listener->BuildStart(opts.str());
    }

    fs::path model_path = fs::absolute(fs::path(path));
    fs::path fwd_model_filename = model_path.parent_path() / fs::path("fwd_model.tmp");
    fs::path bwd_model_filename = model_path.parent_path() / fs::path("bwd_model.tmp");

    if (listener) listener->VocabularyBuildBegin();
    Vocabulary vocab(case_sensitive);
    vocab.BuildFromCorpora(corpora, max_length, vocabulary_threshold);
    if (listener) listener->VocabularyBuildEnd();

    auto *forward = (BuilderModel *) BuildModel(vocab, corpora, true);
    forward->Store(fwd_model_filename.string());
    delete forward;

    auto *backward = (BuilderModel *) BuildModel(vocab, corpora, false);
    backward->Store(bwd_model_filename.string());
    delete backward;

    if (listener) listener->ModelDumpBegin();
    ofstream out(model_path.string(), ios::binary | ios::out);
    vocab.Store(out);
    MergeAndStore(out, fwd_model_filename.string(), bwd_model_filename.string());
    out.close();

    if (remove(fwd_model_filename.c_str()) != 0)
        throw runtime_error("Error deleting the forward model file");

    if (remove(bwd_model_filename.c_str()) != 0)
        throw runtime_error("Error deleting the backward model file");

    if (listener) listener->ModelDumpEnd();
}

Model *Builder::BuildModel(const Vocabulary &vocab, const std::vector<Corpus> &corpora, bool forward) {
    auto *model = new BuilderModel(!forward, use_null, favor_diagonal, prob_align_null, initial_diagonal_tension);

    if (listener) listener->Begin(forward);

    vector<pair<pair<length_t, length_t>, size_t>> size_counts;
    double n_target_tokens = 0;

    if (listener) listener->Begin(forward, kBuilderStepSetup, 0);
    InitialPass(vocab, model, corpora, &n_target_tokens, &size_counts);
    if (listener) listener->End(forward, kBuilderStepSetup, 0);

    for (int iter = 0; iter < iterations; ++iter) {
        if (listener) listener->IterationBegin(forward, iter + 1);

        double emp_feat = 0.0;

        vector<pair<wordvec_t, wordvec_t>> batch;

        if (listener) listener->Begin(forward, kBuilderStepAligning, iter + 1);
        for (auto corpus = corpora.begin(); corpus != corpora.end(); ++corpus) {
            CorpusReader reader(*corpus, &vocab, max_length, true);

            while (reader.Read(batch, buffer_size)) {
                emp_feat += model->ComputeAlignments(batch, model, nullptr);
                batch.clear();
            }
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

void Builder::MergeAndStore(std::ostream &out, const string &fwd_path, const string &bwd_path) {
    // creating the bitable
    auto *table = new bitable_t;

    // opening forward model file for reading
    ifstream fwd_in(fwd_path, ios::binary | ios::in);

    // loading forward header
    auto fwd_use_null = io_read<bool>(fwd_in);
    auto fwd_favor_diagonal = io_read<bool>(fwd_in);
    auto fwd_prob_align_null = io_read<double>(fwd_in);
    auto fwd_diagonal_tension = io_read<double>(fwd_in);
    auto fwd_ttable_size = io_read<size_t>(fwd_in);

    if (fwd_ttable_size == 0)
        throw runtime_error("The forward model is empty");

    // resizing the bitable
    table->resize(fwd_ttable_size);

    // loading forward entries and fill the bitable
    while (true) {
        auto src_word = io_read<word_t>(fwd_in);
        if (fwd_in.eof())
            break;

        auto row_size = io_read<size_t>(fwd_in);
        table->at(src_word).reserve(row_size);

        for (size_t i = 0; i < row_size; ++i) {
            auto tgt_word = io_read<word_t>(fwd_in);
            auto score = io_read<float>(fwd_in);

            table->at(src_word)[tgt_word] = pair<float, float>(score, kNullProbability);
        }
    }

    // closing forward model file
    fwd_in.close();

    // opening backward model file for reading
    ifstream bwd_in(bwd_path, ios::binary | ios::in);

    // loading backward header
    auto bwd_use_null = io_read<bool>(bwd_in);
    auto bwd_favor_diagonal = io_read<bool>(bwd_in);
    auto bwd_prob_align_null = io_read<double>(bwd_in);
    auto bwd_diagonal_tension = io_read<double>(bwd_in);
    auto bwd_ttable_size = io_read<size_t>(bwd_in);

    // checking consistency of forward and backward models
    assert(fwd_use_null == bwd_use_null);
    assert(fwd_favor_diagonal == bwd_favor_diagonal);
    assert(fwd_prob_align_null == bwd_prob_align_null);
    assert(fwd_ttable_size == bwd_ttable_size);

    if (bwd_ttable_size == 0)
        throw runtime_error("The backward model is empty");

    // loading backward entries and fill the bitable
    while (true) {
        auto tgt_word = io_read<word_t>(bwd_in);
        if (bwd_in.eof())
            break;

        auto row_size = io_read<size_t>(bwd_in);

        for (size_t i = 0; i < row_size; ++i) {
            auto src_word = io_read<word_t>(bwd_in);
            auto score = io_read<float>(bwd_in);

            assert(src_word < table->size());

            auto cell = table->at(src_word).emplace(tgt_word, pair<float, float>(kNullProbability, kNullProbability));
            pair<float, float> &el = cell.first->second;
            el.second = score;
        }

    }

    // closing backward model file
    bwd_in.close();

    // writing header
    io_write(out, fwd_use_null);
    io_write(out, fwd_favor_diagonal);
    io_write(out, fwd_prob_align_null);
    io_write(out, fwd_diagonal_tension);
    io_write(out, bwd_diagonal_tension);
    io_write(out, fwd_ttable_size);

    // writing all entries of the bitable
    for (word_t src_word = 0; src_word < fwd_ttable_size; ++src_word) {
        auto &row = table->at(src_word);

        io_write(out, src_word);
        io_write(out, row.size());

        for (auto tgt_entry = row.begin(); tgt_entry != row.end(); ++tgt_entry) {
            io_write(out, tgt_entry->first);
            io_write(out, tgt_entry->second.first);
            io_write(out, tgt_entry->second.second);
        }
    }

    // deleting bitable
    delete table;
}
