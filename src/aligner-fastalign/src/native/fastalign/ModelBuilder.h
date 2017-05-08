//
// Created by Davide  Caroselli on 23/08/16.
//

#ifndef FASTALIGN_MODELBUILDER_H
#define FASTALIGN_MODELBUILDER_H

#include <cstddef>
#include <string>
#include "Model.h"
#include "Corpus.h"

using namespace std;

namespace mmt {
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

        typedef int BuilderStep;

        static const BuilderStep kBuilderStepSetup = 1;
        static const BuilderStep kBuilderStepAligning = 2;
        static const BuilderStep kBuilderStepOptimizingDiagonalTension = 3;
        static const BuilderStep kBuilderStepNormalizing = 4;
        static const BuilderStep kBuilderStepPruning = 5;
        static const BuilderStep kBuilderStepStoringModel = 6;

        class ModelBuilder {
        public:

            class Listener {
            public:
                virtual void Begin() = 0;

                virtual void IterationBegin(int iteration) = 0;

                virtual void Begin(const BuilderStep step, int iteration) = 0;

                virtual void End(const BuilderStep step, int iteration) = 0;

                virtual void IterationEnd(int iteration) = 0;

                virtual void End() = 0;
            };

            ModelBuilder(Options options = Options());

            void setListener(Listener *listener);

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

            Listener *listener;

            Model *model;

            void AllocateTTableSpace(ttable_t *table, const unordered_map<wid_t, vector<wid_t>> &values,
                                     const wid_t sourceWordMaxValue);

            void InitialPass(const Corpus &corpus, double *n_target_tokens, ttable_t *table,
                             vector<pair<pair<length_t, length_t>, size_t>> *size_counts);

        };

    }
}

#endif //FASTALIGN_MODELBUILDER_H
