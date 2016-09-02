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

        struct AlignmentStats {
            double emp_feat = 0;
            double c0 = 0;
            double likelihood = 0;
        };

        class Model : public AlignerModel {
            friend class ModelBuilder;

        public:

            static Model *Open(const string &filename);

            virtual inline alignment_t ComputeAlignment(const vector<wid_t> &source, const vector<wid_t> &target) override {
                vector<pair<vector<wid_t>, vector<wid_t>>> batch;
                batch.push_back(pair<vector<wid_t>, vector<wid_t>>(source, target));

                vector<alignment_t> outAlignments;

                ComputeAlignments(batch, NULL, NULL, &outAlignments);

                return outAlignments[0];
            }

            virtual inline void ComputeAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch, vector<alignment_t> &outAlignments) override {
                ComputeAlignments(batch, NULL, NULL, &outAlignments);
            }

            inline void ComputeAlignments(const vector<pair<string, string>> &batch, vector<alignment_t> &outAlignments) {
                ComputeAlignments(batch, NULL, NULL, &outAlignments);
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

            void ComputeAlignments(const vector<pair<string, string>> &batch, ttable_t *outTable = NULL,
                                   AlignmentStats *outStats = NULL, vector<alignment_t> *outAlignments = NULL);

            void ComputeAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch, ttable_t *outTable = NULL,
                                   AlignmentStats *outStats = NULL, vector<alignment_t> *outAlignments = NULL);

            void Store(const string &filename);
        };

    }
}

#endif //FASTALIGN_MODEL_H
