//
// Created by Davide  Caroselli on 23/08/16.
//

#ifndef FASTALIGN_MODELBUILDER_H
#define FASTALIGN_MODELBUILDER_H

#include <cstddef>
#include <string>
#include "Model.h"
#include "TTable.h"
#include "Corpus.h"

using namespace std;

namespace fastalign {

    struct Options {
        double mean_srclen_multiplier = 1.0;
        bool is_reverse;
        int iterations = 5;
        bool favor_diagonal = true;
        double prob_align_null = 0.08;
        double initial_diagonal_tension = 4.0;
        bool optimize_tension = true;
        bool variational_bayes = true;
        double alpha = 0.01;
        bool use_null = true;
        int threads = 0; // Default is number of CPUs
        size_t buffer_size = 10000;

        Options(bool is_reverse = false) : is_reverse(is_reverse) {};
    };

    class ModelBuilder {
    public:
        ModelBuilder(Options options = Options());

        Model *Build(const Corpus &corpus, const string &model_filename);

    private:
        const double mean_srclen_multiplier;
        const bool is_reverse;
        const int iterations;
        const bool favor_diagonal;
        const double prob_align_null;
        const bool optimize_tension;
        const bool variational_bayes;
        const double alpha;
        const bool use_null;
        const size_t buffer_size;
        const int threads;

        Model *model;

        void InitialPass(const Corpus &corpus, double *n_target_tokens,
                         vector<pair<pair<short, short>, unsigned int>> *size_counts);

        void forceAlign(const Corpus &corpus);

    };

}


#endif //FASTALIGN_MODELBUILDER_H
