//
// Created by Davide  Caroselli on 30/09/16.
//

#ifndef SAPT_HASHUTILS_H
#define SAPT_HASHUTILS_H

#include <mmt/sentence.h>

using namespace std;

static inline unsigned int words_hash(const vector<mmt::wid_t> &words, size_t offset, size_t size) {
    unsigned int hash = 1;
    for (size_t i = 0; i < size; ++i)
        hash = 31 * hash + words[offset + i];

    if (hash == 0)
        hash = 1;

    return hash;
}

static inline unsigned int words_hash(const vector<mmt::wid_t> &words) {
    return words_hash(words, 0, words.size());
}

#endif //SAPT_HASHUTILS_H
