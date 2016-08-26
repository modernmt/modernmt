// Copyright 2013 by Chris Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

#ifndef FASTALIGN_TTABLE_H
#define FASTALIGN_TTABLE_H

#include <cassert>
#include <cmath>
#include <fstream>
#include <iostream>
#include <vector>
#include <unordered_map>
#include <atomic>

typedef std::unordered_map<unsigned, double> Word2Double;

struct Md {

    static double digamma(double x) {
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

    static inline double log_poisson(unsigned x, const double &lambda) {
        assert(lambda > 0.0);
        return std::log(lambda) * x - lgamma(x + 1) - lambda;
    }
};

class TTable {
public:

    TTable() : frozen_(false), probs_initialized_(false) {
    }

    typedef std::vector<Word2Double> Word2Word2Double;

    size_t size() {
        return ttable.size();
    }

    inline double prob(const unsigned e, const unsigned f) const {
        return probs_initialized_ ? ttable[e].find(f)->second : 1e-9;
    }

    inline double safe_prob(const unsigned e, const unsigned f) const {
        if (!probs_initialized_)
            return 1e-9;
        if (e < static_cast<unsigned> (ttable.size())) {
            const Word2Double &cpd = ttable[e];
            const Word2Double::const_iterator it = cpd.find(f);
            if (it == cpd.end()) return 1e-9;
            return it->second;
        } else {
            return 1e-9;
        }
    }

    inline void
    Emplace(const std::unordered_map<uint32_t, std::vector<uint32_t>> &values, const uint32_t sourceWordMaxValue) {
        if (counts.size() < sourceWordMaxValue) {
#pragma omp critical(resize_counts)
            {
                if (counts.size() < sourceWordMaxValue)
                    counts.resize(sourceWordMaxValue + 1);
            }
        }

#pragma omp parallel for schedule(dynamic)
        for (size_t bucket = 0; bucket < values.bucket_count(); ++bucket) {
            for (auto row_ptr = values.begin(bucket); row_ptr != values.end(bucket); ++row_ptr) {
                uint32_t sourceWord = row_ptr->first;

                for (auto targetWord = row_ptr->second.begin(); targetWord != row_ptr->second.end(); ++targetWord)
                    counts[sourceWord][*targetWord] = 0;
            }
        }
    }

    inline void Update(const uint32_t sourceWord, const uint32_t targetWord, const double update) {
#pragma omp atomic
        counts[sourceWord][targetWord] += update;
    }

    void NormalizeVB(const double alpha);

    void Normalize();

    void Freeze();

    void LoadFromBinFile(std::ifstream &in, double &DiagonalTension, double &MeanSourceLenMultiplier);

    void SaveToBinFile(std::ofstream &out, double DiagonalTension, double MeanSourceLenMultiplier) const;

    void Prune(double threshold);

private:
    void ClearCounts();

    Word2Word2Double ttable;
    Word2Word2Double counts;
    bool frozen_; // Disallow new e,f pairs to be added to counts
    bool probs_initialized_; // If we can use the values in probs


};


#endif //FASTALIGN_TTABLE_H
