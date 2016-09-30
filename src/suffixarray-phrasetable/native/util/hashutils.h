//
// Created by Davide  Caroselli on 30/09/16.
//

#ifndef SAPT_HASHUTILS_H
#define SAPT_HASHUTILS_H

#include <mmt/sentence.h>

using namespace std;

static inline unsigned int words_hash(const vector<mmt::wid_t> &words) {
    unsigned int hash = 1;
    for (size_t i = 0; i < words.size(); ++i)
        hash = 31 * hash + words[i];

    return hash;
}

#endif //SAPT_HASHUTILS_H
