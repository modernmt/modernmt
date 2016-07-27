//
// Created by Davide Caroselli on 25/07/16.
//

#ifndef MOSESDECODER_VOCABULARY_H
#define MOSESDECODER_VOCABULARY_H

#include <string>
#include <rocksdb/db.h>
#include <unordered_set>
#include "IdGenerator.h"
#include "NewWordOperator.h"

const uint32_t kVocabularyUnknownWord = 0;
const uint32_t kVocabularyWordIdStart = 1000;

using namespace std;

class Vocabulary {
    friend class NewWordOperator;
    friend class VocabularyBuilder;
public:
    Vocabulary(string path, bool prepareForBulkLoad = false);

    ~Vocabulary();

    uint32_t Lookup(string &word, bool putIfAbsent);

    void Lookup(vector<vector<string>> &buffer, vector<vector<uint32_t>> &output, bool putIfAbsent);

    bool ReverseLookup(uint32_t id, string *output);

private:
    string idGeneratorPath;
    IdGenerator idGenerator;
    rocksdb::DB* directDb;
    rocksdb::DB* reverseDb;

    void Put(const string &word, const uint32_t id);
    void ForceCompaction();

    static const void Serialize(uint32_t value, uint8_t *output);
    static const bool Deserialize(string &value, uint32_t *output);
    static const bool Deserialize(const rocksdb::Slice &value, uint32_t *output);
};

#endif //MOSESDECODER_VOCABULARY_H
