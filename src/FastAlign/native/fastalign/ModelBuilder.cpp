//
// Created by Davide  Caroselli on 23/08/16.
//

#include <iostream>
#include <sstream>
#include <thread>
#include <omp.h>
#include "DiagonalAlignment.h"
#include "ModelBuilder.h"

#include <sys/time.h>

using namespace fastalign;

void printAlignment(vector<alignment> &alignments) {
    for (auto a = alignments.begin(); a != alignments.end(); ++a) {
        for (size_t i = 0; i < a->size(); ++i) {
            if (i > 0)
                cout << ' ';
            cout << a->at(i).first << '-' << a->at(i).second;
        }

        cout << endl;
    }
}

void ModelBuilder::forceAlign(const Corpus &corpus) {
    CorpusReader reader(corpus);
    vector<pair<string, string>> batch;
    vector<alignment> alignments;

    while (reader.ReadLines(batch, buffer_size)) {
        model->ComputeAlignments(batch, false, NULL, &alignments);

        printAlignment(alignments);

        alignments.clear();
        batch.clear();
    }
}


struct PairHash {
    size_t operator()(const pair<short, short> &x) const {
        return (unsigned short) x.first << 16 | (unsigned) x.second;
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
                                              threads((options.threads == 0) ? thread::hardware_concurrency()
                                                                             : options.threads),
                                              buffer_size(options.buffer_size) {
    if (variational_bayes && alpha <= 0.0)
        throw invalid_argument("Parameter 'alpha' must be greather than 0");

    model = new Model(is_reverse, use_null, favor_diagonal, prob_align_null, options.initial_diagonal_tension);
}

void ModelBuilder::InitialPass(const Corpus &corpus, double *n_target_tokens,
                               vector<pair<pair<short, short>, unsigned int>> *size_counts) {
    CorpusReader reader(corpus);

    unordered_map<pair<short, short>, unsigned, PairHash> size_counts_;

    unordered_map<word, vector<word>> buffer;
    word maxSourceWord = 0;
    size_t buffer_items = 0;
    sentence src, trg;

    while (reader.Read(src, trg)) {
        if (is_reverse)
            swap(src, trg);

        *n_target_tokens += trg.size();

        if (use_null) {
            for (size_t idxf = 0; idxf < trg.size(); ++idxf) {
                buffer[kNullWord].push_back(trg[idxf]);
            }

            buffer_items += trg.size();
        }

        for (unsigned idxe = 0; idxe < src.size(); ++idxe) {
            for (unsigned idxf = 0; idxf < trg.size(); ++idxf) {
                maxSourceWord = max(maxSourceWord, src[idxe]);
                buffer[src[idxe]].push_back(trg[idxf]);
            }
            buffer_items += trg.size();
        }

        if (buffer_items > buffer_size * 100) {
            model->s2t.Emplace(buffer, maxSourceWord);
            buffer_items = 0;
            maxSourceWord = 0;
            buffer.clear();
        }

        ++size_counts_[make_pair<short, short>(trg.size(), src.size())];
    }

    for (unordered_map<pair<short, short>, unsigned, PairHash>::const_iterator p = size_counts_.begin();
         p != size_counts_.end(); ++p) {
        size_counts->push_back(*p);
    }

    model->s2t.Emplace(buffer, maxSourceWord);
}

double get_wall_time() {
    struct timeval time;

    if (gettimeofday(&time, NULL)) {
        //  Handle error
        return 0;
    }

    return (double) time.tv_sec + ((double) time.tv_usec / 1000000.);
}

#define clock_start(msg) {cerr << msg << "..."; begin = get_wall_time();}
#define clock_stop() {cerr << "DONE in " << (get_wall_time() - begin) << endl;}

Model *ModelBuilder::Build(const Corpus &corpus, const string &model_filename) {
    double begin;
    omp_set_dynamic(0);
    omp_set_num_threads(threads);

    vector<pair<pair<short, short>, unsigned >> size_counts;
    double n_target_tokens = 0;


    clock_start("Initial step");
    InitialPass(corpus, &n_target_tokens, &size_counts);
    clock_stop()

    model->s2t.Freeze();

    for (int iter = 0; iter < iterations; ++iter) {
        cerr << "Iteration " << (iter + 1) << ":" << endl;

        AlignmentStats stats;

        CorpusReader reader(corpus);
        vector<pair<string, string>> batch;

        clock_start("   Aligning");
        while (reader.ReadLines(batch, buffer_size)) {
            model->ComputeAlignments(batch, true, &stats);
            batch.clear();
        }
        clock_stop()

        stats.emp_feat /= n_target_tokens;

        if (iter < iterations - 1) { // TODO remove, why wasting last iteration?
            if (favor_diagonal && optimize_tension) {
                clock_start("   Optimizing diagonal tension");
                for (int ii = 0; ii < 8; ++ii) {
                    double mod_feat = 0;
#pragma omp parallel for reduction(+:mod_feat)
                    for (size_t i = 0; i < size_counts.size(); ++i) {
                        const pair<short, short> &p = size_counts[i].first;
                        for (short j = 1; j <= p.first; ++j)
                            mod_feat += size_counts[i].second *
                                        DiagonalAlignment::ComputeDLogZ(j, p.first, p.second, model->diagonal_tension);
                    }
                    mod_feat /= n_target_tokens;
                    model->diagonal_tension += (stats.emp_feat - mod_feat) * 20.0;
                    if (model->diagonal_tension <= 0.1) model->diagonal_tension = 0.1;
                    if (model->diagonal_tension > 14) model->diagonal_tension = 14;
                }
                clock_stop()
            }

            clock_start("   Normalizing");
            if (variational_bayes)
                model->s2t.NormalizeVB(alpha);
            else
                model->s2t.Normalize();
            clock_stop()
        }
    }

    clock_start("Pruning");
    model->s2t.Prune(1e-20);
    clock_stop()

    clock_start("Storing model");
    if (!model_filename.empty()) {
        std::ofstream out(model_filename, ios::binary | ios::out);
        assert(out);
        model->s2t.SaveToBinFile(out, model->diagonal_tension, mean_srclen_multiplier);
        out.close();
    }
    clock_stop()

    clock_start("Printing alignments");
    forceAlign(corpus);
    clock_stop()

    return model;
}
