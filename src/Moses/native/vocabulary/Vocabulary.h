//
// Created by Davide Caroselli on 25/07/16.
//

#ifndef MOSESDECODER_VOCABULARY_H
#define MOSESDECODER_VOCABULARY_H

#include <string>
#include <rocksdb/db.h>
#include "IdGenerator.h"
#include "NewWordOperator.h"

const uint32_t kVocabularyWordIdStart = 1000;

const uint32_t kVocabularyNoWord = 0;
const uint32_t kVocabularyStartSymbol = 1;
const uint32_t kVocabularyEndSymbol = 2;

using namespace std;

class Vocabulary {
    friend class NewWordOperator;
public:
    Vocabulary(string path);

    ~Vocabulary();

    uint32_t Get(string word, bool putIfAbsent);

    bool Get(uint32_t id, string *output);

private:
    string idGeneratorPath;
    IdGenerator idGenerator;
    rocksdb::DB* directDb;
    rocksdb::DB* reverseDb;

    static const void Serialize(uint32_t value, uint8_t *output);
    static const bool Deserialize(string &value, uint32_t *output);
    static const bool Deserialize(const rocksdb::Slice &value, uint32_t *output);
};


#endif //MOSESDECODER_VOCABULARY_H
