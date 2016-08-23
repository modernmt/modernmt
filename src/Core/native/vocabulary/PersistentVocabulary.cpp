//
// Created by Davide  Caroselli on 17/08/16.
//

#include "PersistentVocabulary.h"
#include <rocksdb/table.h>
#include <rocksdb/cache.h>
#include <rocksdb/filter_policy.h>
#include <rocksdb/memtablerep.h>
#include <rocksdb/slice_transform.h>
#include <rocksdb/merge_operator.h>
#include <thread>

#define MakeSlice(buffer) (Slice((const char *) buffer, 4))

const string kPathSeparator =
#ifdef _WIN32
        "\\";
#else
        "/";
#endif

using namespace rocksdb;

static const void Serialize(uint32_t value, uint8_t *output) {
    output[0] = (uint8_t) (value & 0x000000FF);
    output[1] = (uint8_t) ((value & 0x0000FF00) >> 8);
    output[2] = (uint8_t) ((value & 0x00FF0000) >> 16);
    output[3] = (uint8_t) ((value & 0xFF000000) >> 24);
}

static const bool Deserialize(string &value, uint32_t *output) {
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

static const bool Deserialize(const rocksdb::Slice &value, uint32_t *output) {
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

// Operator

class NewWordOperator : public rocksdb::MergeOperator {
public:
    virtual bool FullMerge(const rocksdb::Slice &key, const rocksdb::Slice *existing_value,
                           const std::deque<std::string> &operand_list, std::string *new_value,
                           rocksdb::Logger *logger) const {
        if (existing_value == nullptr) {
            uint32_t maxId = 0;

            for (auto i = operand_list.begin(); i != operand_list.end(); ++i) {
                uint32_t temp;
                uint32_t id = Deserialize(*i, &temp) ? temp : 0;

                if (id > maxId)
                    maxId = id;
            }

            uint8_t buffer[4];
            Serialize(maxId, buffer);
            *new_value = std::string(reinterpret_cast<char const *>(buffer), 4);
        }

        return true;
    }

    virtual bool
    PartialMerge(const rocksdb::Slice &key, const rocksdb::Slice &left_operand, const rocksdb::Slice &right_operand,
                 std::string *new_value, rocksdb::Logger *logger) const {
        uint32_t temp;
        uint32_t id1 = Deserialize(left_operand, &temp) ? temp : 0;
        uint32_t id2 = Deserialize(right_operand, &temp) ? temp : 0;

        uint8_t buffer[4];
        Serialize((id1 > id2 ? id1 : id2), buffer);
        *new_value = std::string(reinterpret_cast<char const *>(buffer), 4);

        return true;
    }

    virtual bool
    PartialMergeMulti(const rocksdb::Slice &key, const deque<rocksdb::Slice, allocator<rocksdb::Slice>> &operand_list,
                      std::string *new_value, rocksdb::Logger *logger) const {
        uint32_t maxId = 0;

        for (auto i = operand_list.begin(); i != operand_list.end(); ++i) {
            uint32_t temp;
            uint32_t id = Deserialize(*i, &temp) ? temp : 0;

            if (id > maxId)
                maxId = id;
        }

        uint8_t buffer[4];
        Serialize(maxId, buffer);
        *new_value = std::string(reinterpret_cast<char const *>(buffer), 4);

        return true;
    }

    virtual const char *Name() const {
        return "NewWordOperator";
    }
};

// PersistentVocabulary implementation

PersistentVocabulary::PersistentVocabulary(string basepath, bool prepareForBulkLoad) :
        idGeneratorPath(basepath + kPathSeparator + "_id"), idGenerator(idGeneratorPath) {
    rocksdb::Options options;
    options.create_if_missing = true;
    options.merge_operator.reset(new NewWordOperator());
    options.prefix_extractor.reset(NewNoopTransform());

    options.table_factory.reset(NewPlainTableFactory());
    options.memtable_factory.reset(NewHashLinkListRepFactory());

    options.allow_mmap_reads = true;
    options.max_open_files = -1;
    options.compaction_style = kCompactionStyleLevel;
    options.memtable_prefix_bloom_bits = 1024 * 1024 * 8;

    if (prepareForBulkLoad) {
        options.PrepareForBulkLoad();
    } else {
        unsigned cpus = thread::hardware_concurrency();

        if (cpus > 1)
            options.IncreaseParallelism(cpus > 4 ? 4 : 2);

        options.level0_file_num_compaction_trigger = 10;
        options.level0_slowdown_writes_trigger = 20;
        options.level0_stop_writes_trigger = 40;

        options.write_buffer_size = 64 * 1024 * 1024;
        options.target_file_size_base = 64 * 1024 * 1024;
        options.max_bytes_for_level_base = 512 * 1024 * 1024;
    }

    string directPath = basepath + kPathSeparator + "direct";
    string reversePath = basepath + kPathSeparator + "reverse";

    Status status = DB::Open(options, directPath, &directDb);
    assert(status.ok());

    status = DB::Open(options, reversePath, &reverseDb);
    assert(status.ok());

    ForceCompaction();
}

uint32_t PersistentVocabulary::Lookup(const string &word, bool putIfAbsent) {
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
                Serialize(id, buffer);

                status = directDb->Merge(WriteOptions(), key, MakeSlice(buffer));
                assert(status.ok());

                status = directDb->Get(options, key, &value);
                assert(status.ok());

                status = reverseDb->Put(WriteOptions(), Slice(value), key);
                assert(status.ok());
            } else {
                return kVocabularyUnknownWord;
            }
        } else {
            assert(false);
        }
    }

    uint32_t output;
    return Deserialize(value, &output) ? output : kVocabularyUnknownWord;
}

void
PersistentVocabulary::Lookup(const vector<vector<string>> &buffer, vector<vector<uint32_t>> *output, bool putIfAbsent) {
    unordered_map<string, uint32_t> vocabulary(buffer.size() * 20);

    for (auto line = buffer.begin(); line != buffer.end(); ++line) {
        size_t length = line->size();

        vector<uint32_t> encoded;
        for (size_t i = 0; i < length; ++i) {
            uint32_t id;
            const string &word = line->at(i);

            auto valueRef = vocabulary.find(word);
            if (valueRef != vocabulary.end()) {
                id = valueRef->second;
            } else {
                id = Lookup(word, putIfAbsent);
                vocabulary[word] = id;
            }

            if (output)
                encoded.push_back(id);
        }

        if (output)
            output->push_back(encoded);
    }
}

const bool PersistentVocabulary::ReverseLookup(uint32_t id, string *output) {
    if (id < kVocabularyWordIdStart)
        return true;

    ReadOptions options = ReadOptions();
    options.verify_checksums = false;

    uint8_t buffer[4];
    Serialize(id, buffer);

    Status status = reverseDb->Get(options, MakeSlice(buffer), output);

    if (!status.ok()) {
        if (status.IsNotFound()) {
            return false;
        }

        assert(false);
    }

    return true;
}

const bool PersistentVocabulary::ReverseLookup(const vector<vector<uint32_t>> &buffer, vector<vector<string>> &output) {
    unordered_map<uint32_t, string> vocabulary(buffer.size() * 20);

    for (auto line = buffer.begin(); line != buffer.end(); ++line) {
        size_t length = line->size();

        vector<string> decoded;
        for (size_t i = 0; i < length; ++i) {
            uint32_t id = line->at(i);
            string word;

            auto valueRef = vocabulary.find(id);
            if (valueRef != vocabulary.end()) {
                word = valueRef->second;
            } else {
                if (!ReverseLookup(id, &word))
                    return false;
                vocabulary[id] = word;
            }

            decoded.push_back(word);
        }

        output.push_back(decoded);
    }
}

void PersistentVocabulary::Put(const string &_word, const uint32_t _id) {
    uint8_t buffer[4];
    Serialize(_id, buffer);

    Slice word(_word);
    Slice id((const char *) buffer, 4);

    Status status = directDb->Put(WriteOptions(), word, id);
    assert(status.ok());

    status = reverseDb->Put(WriteOptions(), id, word);
    assert(status.ok());
}

void PersistentVocabulary::ResetId(uint32_t id) {
    idGenerator.Reset(id);
}

void PersistentVocabulary::ForceCompaction() {
    directDb->CompactRange(CompactRangeOptions(), NULL, NULL);
    reverseDb->CompactRange(CompactRangeOptions(), NULL, NULL);
}

PersistentVocabulary::~PersistentVocabulary() {
    delete directDb;
    delete reverseDb;
}
