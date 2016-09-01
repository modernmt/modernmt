//
// Created by Davide Caroselli on 25/07/16.
//

#ifndef MOSESDECODER_VOCABULARY_H
#define MOSESDECODER_VOCABULARY_H

#include <string>
#include <vector>

const uint32_t kVocabularyUnknownWord = 0;
const uint32_t kVocabularyWordIdStart = 1000;

using namespace std;

class Vocabulary {
    friend class InMemoryVocabulary;
public:
    virtual uint32_t Lookup(const string &word, bool putIfAbsent) = 0;

    virtual void Lookup(const vector<vector<string>> &buffer, vector<vector<uint32_t>> *output, bool putIfAbsent) = 0;

    virtual const bool ReverseLookup(uint32_t id, string *output) = 0;

    virtual const bool ReverseLookup(const vector<vector<uint32_t>> &buffer, vector<vector<string>> &output) = 0;
};

#endif //MOSESDECODER_VOCABULARY_H
