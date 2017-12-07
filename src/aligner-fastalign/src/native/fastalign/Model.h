//
// Created by Davide  Caroselli on 23/08/16.
//

#ifndef FASTALIGN_MODEL_H
#define FASTALIGN_MODEL_H

#include <string>
#include <vector>
#include <unordered_map>
#include "alignment.h"

namespace mmt {
    namespace fastalign {

        const double kNullProbability = 1e-9;

        class Model {
            friend class Builder;

        public:
            Model(bool is_reverse, bool use_null, bool favor_diagonal, double prob_align_null, double diagonal_tension);

            inline alignment_t ComputeAlignment(const wordvec_t &source, const wordvec_t &target) {
                alignment_t alignment;
                ComputeAlignment(source, target, NULL, &alignment);
                return alignment;
            }

            inline void ComputeAlignments(const std::vector<std::pair<wordvec_t, wordvec_t>> &batch,
                                          std::vector<alignment_t> &outAlignments) {
                ComputeAlignments(batch, NULL, &outAlignments);
            }

            void ComputeScores(const std::vector<std::pair<wordvec_t, wordvec_t>> &batch,
                               std::vector<double> &outScores);

            virtual double GetProbability(word_t source, word_t target) = 0;

            virtual void IncrementProbability(word_t source, word_t target, double amount) = 0;

            virtual ~Model() {};

        protected:
            const bool is_reverse;
            const bool use_null;
            const bool favor_diagonal;
            const double prob_align_null;

            double diagonal_tension;

            double ComputeAlignment(const wordvec_t &source, const wordvec_t &target, Model *outModel,
                                    alignment_t *outAlignment, double *outScore = NULL);

            double ComputeAlignments(const std::vector<std::pair<wordvec_t, wordvec_t>> &batch,
                                     Model *outModel, std::vector<alignment_t> *outAlignments);
        };

    }
}

#endif //FASTALIGN_MODEL_H
