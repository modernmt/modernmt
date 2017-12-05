//
// Created by Davide Caroselli on 04/09/16.
//

#ifndef FASTALIGN_ALIGNER_H
#define FASTALIGN_ALIGNER_H

#include <string>
#include "Model.h"
#include "Vocabulary.h"

namespace mmt {
    namespace fastalign {

        enum Symmetrization {
            GrowDiagonalFinalAnd = 1,
            GrowDiagonal = 2,
            Intersection = 3,
            Union = 4
        };

        class FastAligner {
        public:
            const Vocabulary *vocabulary;

            FastAligner(const std::string &path, int threads = 0);

            alignment_t GetAlignment(const sentence_t &source, const sentence_t &target, Symmetrization symmetrization);

            alignment_t GetAlignment(const wordvec_t &source, const wordvec_t &target, Symmetrization symmetrization);

            void GetAlignments(const std::vector<std::pair<sentence_t, sentence_t>> &batch,
                               std::vector<alignment_t> &outAlignments, Symmetrization symmetrization);

            void GetAlignments(const std::vector<std::pair<wordvec_t, wordvec_t>> &batch,
                               std::vector<alignment_t> &outAlignments, Symmetrization symmetrization);

            void GetScores(const std::vector<std::pair<sentence_t, sentence_t>> &batch, std::vector<double> &outScores);

            void GetScores(const std::vector<std::pair<wordvec_t, wordvec_t>> &batch, std::vector<double> &outScores);

            virtual ~FastAligner();

        private:
            Model *forwardModel;
            Model *backwardModel;

            int threads;
        };

    }
}


#endif //FASTALIGN_ALIGNER_H
