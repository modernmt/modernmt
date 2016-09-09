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
#include <mmt/aligner/AlignerModel.h>

using namespace std;

namespace mmt {
    namespace fastalign {

        const double kNullProbability = 1e-9;

        typedef vector<unordered_map<wid_t, double>> ttable_t;

        class Model : public AlignerModel {
            friend class ModelBuilder;

        public:

            static Model *Open(const string &filename);

            virtual inline alignment_t
            ComputeAlignment(const vector<wid_t> &source, const vector<wid_t> &target) override {
                alignment_t alignment;
                ComputeAlignment(source, target, NULL, &alignment);
                return alignment;
            }

            virtual inline void ComputeAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch,
                                                  vector<alignment_t> &outAlignments) override {
                ComputeAlignments(batch, NULL, &outAlignments);
            }

            inline double GetProbability(wid_t source, wid_t target) {
                if (translation_table.empty())
                    return kNullProbability;
                if (source >= translation_table.size())
                    return kNullProbability;

                unordered_map<wid_t, double> &row = translation_table[source];
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

            double ComputeAlignment(const vector<wid_t> &source, const vector<wid_t> &target, ttable_t *outTable,
                                    alignment_t *outAlignment);

            double ComputeAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch, ttable_t *outTable,
                                     vector<alignment_t> *outAlignments);

            void Store(const string &filename);
        };

    }
}

#endif //FASTALIGN_MODEL_H
