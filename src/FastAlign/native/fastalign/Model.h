//
// Created by Davide  Caroselli on 23/08/16.
//

#ifndef FASTALIGN_MODEL_H
#define FASTALIGN_MODEL_H

#include <cstdint>
#include <utility>
#include <string>
#include <vector>
#include <unordered_map>
#include "TTable.h"

using namespace std;

namespace fastalign {

    typedef uint32_t word;
    typedef vector<uint32_t> sentence;
    typedef vector<pair<word, word>> alignment;

    static const word kNullWord = 0;
    static const double kNullProbability = 1e-9;

    struct AlignmentStats {
        double emp_feat = 0;
        double c0 = 0;
        double likelihood = 0;
    };

    class Model {
        friend class ModelBuilder;

    public:


    private:
        TTable s2t;

        const bool is_reverse;
        const bool use_null;
        const bool favor_diagonal;
        const double prob_align_null;

        double diagonal_tension;

        Model(const bool is_reverse, const bool use_null, const bool favor_diagonal, const double prob_align_null,
              double diagonal_tension);

        void ComputeAlignments(vector<pair<sentence, sentence>> batch, bool updateModel,
                               AlignmentStats *outStats = NULL, vector<alignment> *outAlignments = NULL);
    };

}


#endif //FASTALIGN_MODEL_H
