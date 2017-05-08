//
// Created by Davide  Caroselli on 23/08/16.
//

#ifndef FASTALIGN_MODEL_H
#define FASTALIGN_MODEL_H

#include <cstdint>
#include <utility>
#include <string>
#include <vector>
#include <math.h>
#include <unordered_map>
#include <mmt/sentence.h>

using namespace std;

namespace mmt {
    namespace fastalign {

        const double kNullProbability = 1e-9;

        struct ttable_t {

            virtual double get(wid_t source, wid_t target) = 0;

            virtual void increment(wid_t source, wid_t target, double amount) = 0;

            virtual ~ttable_t() {};

        };

        inline double digamma(double x) {
            double result = 0, xx, xx2, xx4;
            for (; x < 7; ++x)
                result -= 1 / x;
            x -= 1.0 / 2.0;
            xx = 1.0 / x;
            xx2 = xx * xx;
            xx4 = xx2 * xx2;
            result += log(x) + (1. / 24.) * xx2 - (7.0 / 960.0) * xx4 + (31.0 / 8064.0) * xx4 * xx2 -
                      (127.0 / 30720.0) * xx4 * xx4;
            return result;
        }

        //TODO: must be private
        struct builder_ttable_t : ttable_t {
            vector<unordered_map<wid_t, pair<double, double>>> data;

            double get(wid_t source, wid_t target) override {
                if (data.empty())
                    return kNullProbability;
                if (source >= data.size())
                    return kNullProbability;

                unordered_map<wid_t, pair<double, double>> &row = data[source];
                auto ptr = row.find(target);
                return ptr == row.end() ? kNullProbability : ptr->second.first;
            }

            void increment(wid_t source, wid_t target, double amount) override {
#pragma omp atomic
                data[source][target].second += amount;
            }

            void prune(double threshold = 1e-20) {
#pragma omp parallel for schedule(dynamic)
                for (size_t i = 0; i < data.size(); ++i) {
                    unordered_map<wid_t, pair<double, double>> &row = data[i];

                    for (auto cell = row.cbegin(); cell != row.cend(); /* no increment */) {
                        if (cell->second.first <= threshold)
                            row.erase(cell++);
                        else
                            ++cell;
                    }
                }
            }

            void normalize(double alpha = 0) {
                for (size_t i = 0; i < data.size(); ++i) {
                    unordered_map<wid_t, pair<double, double>> &row = data[i];
                    double row_norm = 0;

                    for (auto cell = row.begin(); cell != row.end(); ++cell)
                        row_norm += cell->second.first + alpha;

                    if (row_norm == 0) row_norm = 1;

                    if (alpha > 0)
                        row_norm = digamma(row_norm);

                    for (auto cell = row.begin(); cell != row.end(); ++cell)
                        cell->second.first =
                                alpha > 0 ?
                                exp(digamma(cell->second.first + alpha) - row_norm) :
                                cell->second.first / row_norm;
                }
            }

            void swap() {
#pragma omp parallel for schedule(dynamic)
                for (size_t i = 0; i < data.size(); ++i) {
                    for (auto cell = data[i].begin(); cell != data[i].end(); ++cell) {
                        cell->second.first = cell->second.second;
                        cell->second.second = 0;
                    }
                }
            }
        };

        class Model {
            friend class ModelBuilder;

        public:
            Model(ttable_t *translation_table, bool is_reverse, bool use_null, bool favor_diagonal,
                  double prob_align_null, double diagonal_tension);

            inline alignment_t
            ComputeAlignment(const vector<wid_t> &source, const vector<wid_t> &target) {
                alignment_t alignment;
                ComputeAlignment(source, target, NULL, &alignment);
                return alignment;
            }

            inline void ComputeAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch,
                                          vector<alignment_t> &outAlignments) {
                ComputeAlignments(batch, NULL, &outAlignments);
            }

            inline double GetProbability(wid_t source, wid_t target) {
                return translation_table->get(source, target);
            }

            virtual ~Model() {
                delete translation_table;
            }

        private:
            ttable_t *translation_table;

            const bool is_reverse;
            const bool use_null;
            const bool favor_diagonal;
            const double prob_align_null;

            double diagonal_tension;

            double ComputeAlignment(const vector<wid_t> &source, const vector<wid_t> &target, ttable_t *outTable,
                                    alignment_t *outAlignment);

            double ComputeAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch, ttable_t *outTable,
                                     vector<alignment_t> *outAlignments);
        };

    }
}

#endif //FASTALIGN_MODEL_H
