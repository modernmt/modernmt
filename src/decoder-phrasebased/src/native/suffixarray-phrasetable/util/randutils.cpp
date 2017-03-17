//
// Created by Davide Caroselli on 02/10/16.
//

#include "randutils.h"
#include <unordered_set>
#include <random>
#include <algorithm>
#include <functional>

void GenerateRandomSequence(size_t size, size_t limit, unsigned int seed, vector<size_t> &outSequence) {
    // Implementation taken from StackOverflow thread:
    // http://stackoverflow.com/questions/6947612/generating-m-distinct-random-numbers-in-the-range-0-n-1

    mt19937 random_engine(seed);

    double n = size;
    double T = 0; // T = Sum[1 / (n - k)], k = 0 --> m - 1
    for (double k = 0.; k < limit; k++)
        T += 1. / (n - k);

    if (T < (n - 1.) / n) {
        outSequence.resize(limit);
        unordered_set<int> coveredPositions;

        auto rand_generator = bind(uniform_int_distribution<int>(0, (int) (size - 1)), random_engine);

        for (size_t i = 0; i < limit; ++i) {
            int index;

            do {
                index = rand_generator();
            } while (coveredPositions.find(index) != coveredPositions.end());

            outSequence[i] = (size_t) index;
        }
    } else {
        outSequence.resize(size);
        for (size_t i = 0; i < size; ++i)
            outSequence[i] = i;

        shuffle(outSequence.begin(), outSequence.end(), random_engine);
        outSequence.resize(limit);
    }
}