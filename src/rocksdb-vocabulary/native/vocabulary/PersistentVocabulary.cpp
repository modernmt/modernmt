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
using namespace mmt;
using namespace mmt::vocabulary;

static_assert(sizeof(wid_t) == 4, "Current implementation only support 4-byte word id");

static const void Serialize(wid_t value, uint8_t *output) {
    output[0] = (uint8_t) (value & 0x000000FF);
    output[1] = (uint8_t) ((value & 0x0000FF00) >> 8);
    output[2] = (uint8_t) ((value & 0x00FF0000) >> 16);
    output[3] = (uint8_t) ((value & 0xFF000000) >> 24);
}

static const bool Deserialize(string &value, wid_t *output) {
    if (value.size() != sizeof(wid_t))
        return false;

    const char *buffer = value.data();

    wid_t id = buffer[0] & 0xFFU;
    id += (buffer[1] & 0xFFU) << 8;
    id += (buffer[2] & 0xFFU) << 16;
    id += (buffer[3] & 0xFFU) << 24;

    *output = id;

    return true;
}

static const bool Deserialize(const rocksdb::Slice &value, wid_t *output) {
    if (value.size() != sizeof(wid_t))
        return false;

    const char *buffer = value.data();

    wid_t id = buffer[0] & 0xFFU;
    id += (buffer[1] & 0xFFU) << 8;
    id += (buffer[2] & 0xFFU) << 16;
    id += (buffer[3] & 0xFFU) << 24;

    *output = id;

    return true;
}

// Operator

class NewWordOperator : public MergeOperator {
public:
    virtual bool
    FullMerge(const Slice &key, const Slice *existing_value, const deque<string> &operand_list, string *new_value,
              Logger *logger) const {
        if (existing_value == nullptr) {
            wid_t maxId = 0;

            for (auto i = operand_list.begin(); i != operand_list.end(); ++i) {
                wid_t temp;
                wid_t id = Deserialize(*i, &temp) ? temp : 0;

                if (id > maxId)
                    maxId = id;
            }

            uint8_t buffer[4];
            Serialize(maxId, buffer);
            *new_value = string(reinterpret_cast<char const *>(buffer), 4);
        }

        return true;
    }

    virtual bool
    PartialMerge(const Slice &key, const Slice &left_operand, const Slice &right_operand, string *new_value,
                 Logger *logger) const {
        wid_t temp;
        wid_t id1 = Deserialize(left_operand, &temp) ? temp : 0;
        wid_t id2 = Deserialize(right_operand, &temp) ? temp : 0;

        uint8_t buffer[4];
        Serialize((id1 > id2 ? id1 : id2), buffer);
        *new_value = std::string(reinterpret_cast<char const *>(buffer), 4);

        return true;
    }

    virtual bool
    PartialMergeMulti(const Slice &key, const deque<Slice, allocator<Slice>> &operand_list,
                      string *new_value, Logger *logger) const {
        wid_t maxId = 0;

        for (auto i = operand_list.begin(); i != operand_list.end(); ++i) {
            wid_t temp;
            wid_t id = Deserialize(*i, &temp) ? temp : 0;

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

wid_t PersistentVocabulary::Lookup(const string &word, bool putIfAbsent) {
    ReadOptions options = ReadOptions();
    options.verify_checksums = false;

    Slice key(word);

    string value;
    Status status = directDb->Get(options, key, &value);

    if (!status.ok()) {
        if (status.IsNotFound()) {
            if (putIfAbsent) {
                wid_t id = idGenerator.Next() + kVocabularyWordIdStart;
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

    wid_t output;
    return Deserialize(value, &output) ? output : kVocabularyUnknownWord;
}

void
PersistentVocabulary::Lookup(const vector<vector<string>> &buffer, vector<vector<wid_t>> *output, bool putIfAbsent) {
    unordered_map<string, wid_t> vocabulary(buffer.size() * 20);

    for (auto line = buffer.begin(); line != buffer.end(); ++line) {
        size_t length = line->size();

        vector<wid_t> encoded;
        for (size_t i = 0; i < length; ++i) {
            wid_t id;
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

const bool PersistentVocabulary::ReverseLookup(wid_t id, string *output) {
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

const bool PersistentVocabulary::ReverseLookup(const vector<vector<wid_t>> &buffer, vector<vector<string>> &output) {
    unordered_map<wid_t, string> vocabulary(buffer.size() * 20);

    for (auto line = buffer.begin(); line != buffer.end(); ++line) {
        size_t length = line->size();

        vector<string> decoded;
        for (size_t i = 0; i < length; ++i) {
            wid_t id = line->at(i);
            string word;

            auto valueRef = vocabulary.find(id);
            if (valueRef != vocabulary.end()) {
                word = valueRef->second;
            } else {
                if (!ReverseLookup(id, &word))
                    word = "";
                vocabulary[id] = word;
            }

            decoded.push_back(word);
        }

        output.push_back(decoded);
    }

    return true;
}

void PersistentVocabulary::Put(const string &_word, const wid_t _id) {
    uint8_t buffer[4];
    Serialize(_id, buffer);

    Slice word(_word);
    Slice id((const char *) buffer, 4);

    Status status = directDb->Put(WriteOptions(), word, id);
    assert(status.ok());

    status = reverseDb->Put(WriteOptions(), id, word);
    assert(status.ok());
}

void PersistentVocabulary::ResetId(wid_t id) {
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
