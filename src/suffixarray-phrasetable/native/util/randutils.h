//
// Created by Davide Caroselli on 02/10/16.
//

#ifndef SAPT_RANDUTILS_H
#define SAPT_RANDUTILS_H

#include <cstddef>
#include <vector>

using namespace std;

void GenerateRandomSequence(size_t size, size_t limit, unsigned int seed, vector<size_t> &outSequence);

#endif //SAPT_RANDUTILS_H
