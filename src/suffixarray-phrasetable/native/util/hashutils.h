//
// Created by Davide  Caroselli on 28/09/16.
//

#ifndef SAPT_HASHUTILS_H
#define SAPT_HASHUTILS_H

static inline unsigned int string_hash(const string &string) {
    const char *data = string.data();
    size_t size = string.length();

    unsigned int hash = 1;
    for (size_t i = 0; i < size; ++i)
        hash = 31 * hash + data[i];

    return hash;
}

static inline unsigned int words_hash(const vector<mmt::wid_t> &words) {
    unsigned int hash = 1;
    for (size_t i = 0; i < words.size(); ++i)
        hash = 31 * hash + words[i];

    return hash;
}

#endif //SAPT_HASHUTILS_H
