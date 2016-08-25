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
    vector<pair<sentence, sentence>> batch;
    vector<alignment> alignments;

    while (reader.Read(batch, buffer_size)) {
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

inline void AddTranslationOptions(vector<vector<unsigned> > &insert_buffer, TTable *s2t) {
    s2t->SetMaxE(insert_buffer.size() - 1);
#pragma omp parallel for schedule(dynamic)
    for (unsigned e = 0; e < insert_buffer.size(); ++e) {
        for (unsigned idxf = 0; idxf < insert_buffer[e].size(); ++idxf) {
            s2t->Insert(e, insert_buffer[e][idxf]);
        }
        insert_buffer[e].clear();
    }
}

void ModelBuilder::InitialPass(const Corpus &corpus, double *n_target_tokens, double *tot_len_ratio,
                               vector<pair<pair<short, short>, unsigned int>> *size_counts) {
    CorpusReader reader(corpus);

    unordered_map<pair<short, short>, unsigned, PairHash> size_counts_;
    vector<vector<unsigned >> insert_buffer;
    size_t insert_buffer_items = 0;
    sentence src, trg;

    while (reader.Read(src, trg)) {
        if (is_reverse)
            swap(src, trg);

        *tot_len_ratio += static_cast<double> (trg.size()) / static_cast<double> (src.size());
        *n_target_tokens += trg.size();
        if (use_null) {
            for (unsigned idxf = 0; idxf < trg.size(); ++idxf) {
                model->s2t.Insert(kNullWord, trg[idxf]);
            }
        }
        for (unsigned idxe = 0; idxe < src.size(); ++idxe) {
            if (src[idxe] >= insert_buffer.size()) {
                insert_buffer.resize(src[idxe] + 1);
            }
            for (unsigned idxf = 0; idxf < trg.size(); ++idxf) {
                insert_buffer[src[idxe]].push_back(trg[idxf]);
            }
            insert_buffer_items += trg.size();
        }

        if (insert_buffer_items > buffer_size * 100) {
            insert_buffer_items = 0;
            AddTranslationOptions(insert_buffer, &model->s2t);
        }
        ++size_counts_[make_pair<short, short>(trg.size(), src.size())];
    }

    for (unordered_map<pair<short, short>, unsigned, PairHash>::const_iterator p = size_counts_.begin();
         p != size_counts_.end(); ++p) {
        size_counts->push_back(*p);
    }

    AddTranslationOptions(insert_buffer, &model->s2t);
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
    double tot_len_ratio = 0;
    double n_target_tokens = 0;


    begin = get_wall_time();
    clock_start("Initial step");
    InitialPass(corpus, &n_target_tokens, &tot_len_ratio, &size_counts);
    clock_stop()

    model->s2t.Freeze();

    for (int iter = 0; iter < iterations; ++iter) {
        cerr << "Iteration " << iter << ":" << endl;

        AlignmentStats stats;

        CorpusReader reader(corpus);
        vector<pair<sentence, sentence>> batch;

        clock_start("   Aligning");
        while (reader.Read(batch, buffer_size)) {
            model->ComputeAlignments(batch, true, &stats);
            batch.clear();
        }
        clock_stop()

        stats.emp_feat /= n_target_tokens;

        if (iter < iterations - 1) {
            clock_start("   Calculating diagonal tension");
            if (favor_diagonal && optimize_tension) {
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
            }
            clock_stop()

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
