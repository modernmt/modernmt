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

using namespace std;

namespace fastalign {

    typedef uint32_t word;
    typedef vector<uint32_t> sentence;
    typedef vector<pair<word, word>> alignment;

    static const word kNullWord = 0;
    static const double kNullProbability = 1e-9;

    typedef vector<unordered_map<word, double>> ttable_t;

    struct AlignmentStats {
        double emp_feat = 0;
        double c0 = 0;
        double likelihood = 0;
    };

    class Model {
        friend class ModelBuilder;

    public:

        static Model *Open(const string &filename);

        inline alignment ComputeAlignment(const sentence &source, const sentence &target) {
            vector<pair<sentence, sentence>> batch;
            batch.push_back(pair<sentence, sentence>(source, target));

            vector<alignment> outAlignments;

            ComputeAlignments(batch, NULL, NULL, &outAlignments);

            return outAlignments[0];
        }

        inline void ComputeAlignments(const vector<pair<string, string>> &batch, vector<alignment> &outAlignments) {
            ComputeAlignments(batch, NULL, NULL, &outAlignments);
        }

        inline void ComputeAlignments(const vector<pair<sentence, sentence>> &batch, vector<alignment> &outAlignments) {
            ComputeAlignments(batch, NULL, NULL, &outAlignments);
        }

        inline double GetProbability(word source, word target) {
            if (translation_table.empty())
                return kNullProbability;
            if (source >= translation_table.size())
                return kNullProbability;

            unordered_map<word, double> &row = translation_table[source];
            auto ptr = row.find(target);
            return ptr == row.end() ? kNullProbability : ptr->second;
        }

        void Prune(double threshold = 1e-20);

    private:
        ttable_t translation_table;

        const bool is_reverse;
        const bool use_null;
        const bool favor_diagonal;
        const double prob_align_null;

        double diagonal_tension;

        Model(const bool is_reverse, const bool use_null, const bool favor_diagonal, const double prob_align_null,
              double diagonal_tension);

        void ComputeAlignments(const vector<pair<string, string>> &batch, ttable_t *outTable = NULL,
                               AlignmentStats *outStats = NULL, vector<alignment> *outAlignments = NULL);

        void ComputeAlignments(const vector<pair<sentence, sentence>> &batch, ttable_t *outTable = NULL,
                               AlignmentStats *outStats = NULL, vector<alignment> *outAlignments = NULL);

        void Store(const string &filename);
    };

}


#endif //FASTALIGN_MODEL_H
