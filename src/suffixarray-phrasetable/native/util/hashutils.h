//
// Created by Davide  Caroselli on 28/09/16.
//

#ifndef SAPT_HASHUTILS_H
#define SAPT_HASHUTILS_H

static inline unsigned int string_hash(const string &key) {
    const char *data = key.data();
    size_t size = key.length();

    unsigned int hash = 1;
    for (size_t i = 0; i < size; ++i)
        hash = 31 * hash + data[i];

    return hash;
}

#endif //SAPT_HASHUTILS_H
