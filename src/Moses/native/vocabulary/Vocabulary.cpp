//
// Created by Davide Caroselli on 25/07/16.
//

#include "Vocabulary.h"

const string kPathSeparator =
#ifdef _WIN32
        "\\";
#else
        "/";
#endif

#define MakeSlice(buffer) (Slice((const char *) buffer, 4))

using namespace rocksdb;
using namespace std;

Vocabulary::Vocabulary(string basepath) : idGeneratorPath(basepath + kPathSeparator + "idgen"),
                                          idGenerator(idGeneratorPath) {
    rocksdb::Options options;
    options.create_if_missing = true;
    options.merge_operator.reset(new NewWordOperator());
    options.OptimizeForSmallDb();

    string directPath = basepath + kPathSeparator + "direct";
    string reversePath = basepath + kPathSeparator + "reverse";

    Status status = DB::Open(options, directPath, &directDb);
    assert(status.ok());

    status = DB::Open(options, reversePath, &reverseDb);
    assert(status.ok());
}

Vocabulary::~Vocabulary() {
    delete directDb;
    delete reverseDb;
}

uint32_t Vocabulary::Get(string word, bool putIfAbsent) {
    ReadOptions options = ReadOptions();
    options.verify_checksums = false;

    Slice key(word);

    string value;
    Status status = directDb->Get(options, key, &value);

    if (!status.ok()) {
        if (status.IsNotFound()) {
            if (putIfAbsent) {
                uint32_t id = idGenerator.Next() + kVocabularyWordIdStart;
                uint8_t buffer[4];
                Vocabulary::Serialize(id, buffer);

                status = directDb->Merge(WriteOptions(), key, MakeSlice(buffer));
                assert(status.ok());

                status = directDb->Get(options, key, &value);
                assert(status.ok());

                status = reverseDb->Put(WriteOptions(), Slice(value), key);
                assert(status.ok());
            } else {
                return kVocabularyNoWord;
            }
        } else {
            assert(false);
        }
    }

    uint32_t output;
    return Deserialize(value, &output) ? output : kVocabularyNoWord;
}

bool Vocabulary::Get(uint32_t id, string *output) {
    ReadOptions options = ReadOptions();
    options.verify_checksums = false;

    uint8_t buffer[4];
    Vocabulary::Serialize(id, buffer);

    Status status = reverseDb->Get(options, MakeSlice(buffer), output);

    if (!status.ok()) {
        if (status.IsNotFound()) {
            return false;
        }

        assert(false);
    }

    return true;
}

const void Vocabulary::Serialize(uint32_t value, uint8_t *output) {
    output[0] = (uint8_t)(value & 0x000000FF);
    output[1] = (uint8_t)((value & 0x0000FF00) >> 8);
    output[2] = (uint8_t)((value & 0x00FF0000) >> 16);
    output[3] = (uint8_t)((value & 0xFF000000) >> 24);
}

const bool Vocabulary::Deserialize(string &value, uint32_t *output) {
    if (value.size() != sizeof(uint32_t))
        return false;

    const char *buffer = value.data();

    uint32_t id = buffer[0] & 0xFFU;
    id += (buffer[1] & 0xFFU) << 8;
    id += (buffer[2] & 0xFFU) << 16;
    id += (buffer[3] & 0xFFU) << 24;

    *output = id;

    return true;
}

const bool Vocabulary::Deserialize(const rocksdb::Slice &value, uint32_t *output) {
    if (value.size() != sizeof(uint32_t))
        return false;

    const char *buffer = value.data();

    uint32_t id = buffer[0] & 0xFFU;
    id += (buffer[1] & 0xFFU) << 8;
    id += (buffer[2] & 0xFFU) << 16;
    id += (buffer[3] & 0xFFU) << 24;

    *output = id;

    return true;
}
