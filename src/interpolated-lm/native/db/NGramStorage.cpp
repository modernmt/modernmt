//
// Created by Davide Caroselli on 27/07/16.
//
#include "NGramStorage.h"
#include <rocksdb/memtablerep.h>
#include <rocksdb/table.h>
#include <rocksdb/slice_transform.h>
#include <rocksdb/merge_operator.h>
#include <thread>

#include <iostream>
#include <fstream>

#include "rocksdb/iterator.h"


const string kPathSeparator =
#ifdef _WIN32
        "\\";
#else
        "/";
#endif

#define WordCountsKey (0)
#define StreamsKey SerializeKey(0, 0)

using namespace std;
using namespace rocksdb;
using namespace mmt;
using namespace mmt::ilm;

static_assert(sizeof(seqid_t) == 8, "Current version only supports 64-bit seqid_t");

static inline string SerializeStreams(const vector<seqid_t> &streams) {
    size_t size = streams.size();
    size_t bytes_size = size * sizeof(seqid_t);

    char *bytes = new char[bytes_size];
    size_t j = 0;

    for (size_t i = 0; i < size; ++i) {
        seqid_t value = streams[i];

        bytes[j++] = (char) (value & 0xFF);
        bytes[j++] = (char) ((value >> 8) & 0xFF);
        bytes[j++] = (char) ((value >> 16) & 0xFF);
        bytes[j++] = (char) ((value >> 24) & 0xFF);
        bytes[j++] = (char) ((value >> 32) & 0xFF);
        bytes[j++] = (char) ((value >> 40) & 0xFF);
        bytes[j++] = (char) ((value >> 48) & 0xFF);
        bytes[j++] = (char) ((value >> 56) & 0xFF);
    }

    string result = string(bytes, bytes_size);
    delete[] bytes;
    return result;
}

static inline bool DeserializeStreams(const char *data, size_t bytes_size, vector<seqid_t> &streams) {
    if (bytes_size < 8 || bytes_size % 8 != 0)
        return false;

    size_t size = bytes_size / sizeof(seqid_t);

    streams.resize(size, 0);

    size_t j = 0;
    for (size_t i = 0; i < size; ++i) {
        streams[i] = (data[j++] & 0xFFL) +
                     ((data[j++] & 0xFFL) << 8) +
                     ((data[j++] & 0xFFL) << 16) +
                     ((data[j++] & 0xFFL) << 24) +
                     ((data[j++] & 0xFFL) << 32) +
                     ((data[j++] & 0xFFL) << 40) +
                     ((data[j++] & 0xFFL) << 48) +
                     ((data[j++] & 0xFFL) << 56);
    }

    return true;
}

static inline string SerializeCounts(counts_t counts) {
    const char bytes[8] = {
            (char) (counts.count & 0xFF),
            (char) ((counts.count >> 8) & 0xFF),
            (char) ((counts.count >> 16) & 0xFF),
            (char) ((counts.count >> 24) & 0xFF),

            (char) (counts.successors & 0xFF),
            (char) ((counts.successors >> 8) & 0xFF),
            (char) ((counts.successors >> 16) & 0xFF),
            (char) ((counts.successors >> 24) & 0xFF)
    };

    return string(bytes, 8);
}

static inline string SerializeKey(domain_t domain, dbkey_t key) {
    const char bytes[12] = {
            (char) (domain & 0xFF),
            (char) ((domain >> 8) & 0xFF),
            (char) ((domain >> 16) & 0xFF),
            (char) ((domain >> 24) & 0xFF),

            (char) (key & 0xFF),
            (char) ((key >> 8) & 0xFF),
            (char) ((key >> 16) & 0xFF),
            (char) ((key >> 24) & 0xFF),
            (char) ((key >> 32) & 0xFF),
            (char) ((key >> 40) & 0xFF),
            (char) ((key >> 48) & 0xFF),
            (char) ((key >> 56) & 0xFF)
    };

    return string(bytes, 12);
}

static inline bool DeserializeCounts(const char *data, size_t size, counts_t *output) {
    if (size != 8)
        return false;

    output->count = (data[0] & 0xFFU) +
                    ((data[1] & 0xFFU) << 8) +
                    ((data[2] & 0xFFU) << 16) +
                    ((data[3] & 0xFFU) << 24);

    output->successors = (data[4] & 0xFFU) +
                         ((data[5] & 0xFFU) << 8) +
                         ((data[6] & 0xFFU) << 16) +
                         ((data[7] & 0xFFU) << 24);

    return true;
}

static inline bool DeserializeKey(const char *data, size_t size, domain_t* domain, dbkey_t* key) {
    if (size != 12)
        return false;

    *domain = (data[0] & 0xFFU) +
             ((data[1] & 0xFFU) << 8) +
             ((data[2] & 0xFFU) << 16) +
             ((data[3] & 0xFFU) << 24);

    memcpy(key, data+4, 8);

    return true;
}

class CountsAddOperator : public AssociativeMergeOperator {
public:
    virtual bool Merge(const Slice &key, const Slice *existing_value, const Slice &value, std::string *new_value,
                       Logger *logger) const override {

        counts_t existing;
        if (existing_value)
            DeserializeCounts(existing_value->data_, existing_value->size_, &existing);

        counts_t update;
        DeserializeCounts(value.data_, value.size_, &update);

        existing.count += update.count;
        existing.successors += update.successors;

        *new_value = SerializeCounts(existing);
        return true;
    }

    virtual const char *Name() const override {
        return "CountsAddOperator";
    }
};

NGramStorage::NGramStorage(string basepath, uint8_t order, bool prepareForBulkLoad) throw(storage_exception) : order(
        order) {
    rocksdb::Options options;
    options.create_if_missing = true;
    options.merge_operator.reset(new CountsAddOperator);

    PlainTableOptions plainTableOptions;
    plainTableOptions.user_key_len = sizeof(domain_t) + sizeof(dbkey_t);

    options.prefix_extractor.reset(NewNoopTransform());
    options.table_factory.reset(NewPlainTableFactory(plainTableOptions));
    options.memtable_factory.reset(NewHashLinkListRepFactory());
    options.allow_mmap_reads = true;

    options.max_open_files = -1;
    options.compaction_style = kCompactionStyleLevel;
    options.memtable_prefix_bloom_size_ratio = 1.;

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

    string path = basepath + kPathSeparator + "_data";

    Status status = DB::Open(options, path, &db);
    if (!status.ok())
        throw storage_exception(status.ToString());

    // Read streams
    string raw_streams;
    db->Get(ReadOptions(), StreamsKey, &raw_streams);
    DeserializeStreams(raw_streams.data(), raw_streams.size(), streams);
}

NGramStorage::~NGramStorage() {
    delete db;
}

counts_t NGramStorage::GetCounts(const domain_t domain, const dbkey_t key) const {
    ReadOptions options = ReadOptions(false, true);

    string value;
    Status status = db->Get(options, Slice(SerializeKey(domain, key)), &value);

    if (!status.ok()) {
        if (status.IsNotFound())
            return counts_t();

        return counts_t();
    }

    counts_t output;
    return DeserializeCounts(value.data(), value.size(), &output) ? output : counts_t();
}

void NGramStorage::GetWordCounts(const domain_t domain, count_t *outUniqueWordCount, count_t *outWordCount) const {
    counts_t counts = GetCounts(domain, WordCountsKey);
    if (outWordCount)
        *outWordCount = counts.count;
    if (outUniqueWordCount)
        *outUniqueWordCount = counts.successors;
}

size_t NGramStorage::GetEstimateSize() const {
    uint64_t size;
    db->GetAggregatedIntProperty(Slice("rocksdb.estimate-num-keys"), &size);
    return size;
}

void NGramStorage::PutBatch(NGramBatch &batch) throw(storage_exception) {
    WriteBatch writeBatch;

    unordered_map<domain_t, ngram_table_t> &ngrams = batch.GetNGrams();

    for (auto it = ngrams.begin(); it != ngrams.end(); ++it) {
        PrepareBatch(it->first, it->second, writeBatch);
    }

    // Store streams status
    writeBatch.Put(Slice(StreamsKey), Slice(SerializeStreams(batch.GetStreams())));

    Status status = db->Write(WriteOptions(), &writeBatch);
    if (!status.ok())
        throw storage_exception(status.ToString());

    // Reset streams
    streams = batch.GetStreams();
}

bool NGramStorage::PrepareBatch(domain_t domain, ngram_table_t &table, rocksdb::WriteBatch &writeBatch) {
    // Compute counts (successors and word counts)
    // ------------------------

    // HINT: we start from the maximum order n-grams down to words;
    // if an n-gram is found in the database, we set "is_in_db_for_sure"
    // to all its predecessors (if we have ABC we have for sure BC in the
    // database). This can save lots of read requests for well known n-grams

    ReadOptions read_ops = ReadOptions(false, true);
    Status status;

    // We also store the word count and the count of unique words
    count_t uniqueWordCount = 0;
    count_t wordCount = 0;

    for (size_t o = order; o > 0; --o) {
        unordered_map<dbkey_t, ngram_t> &entry = table[o - 1];

        for (auto it = entry.begin(); it != entry.end(); ++it) {
            dbkey_t key = it->first;
            ngram_t &ngram = it->second;

            if (o == 1)
                wordCount += ngram.counts.count;

            if (!ngram.is_in_db_for_sure) {
                string value;
                status = db->Get(read_ops, Slice(SerializeKey(domain, key)), &value);

                if (!status.ok()) {
                    if (!status.IsNotFound())
                        return false;

                    if (o == 1) { // it is a word
                        uniqueWordCount++;
                    } else {
                        // NGram NOT found in db
                        // increment predecessor's successors
                        table[o - 2][ngram.predecessor].counts.successors++;
                    }
                } else {
                    // If the n-gram is a word, it has no predecessors
                    if (o > 1) {
                        // NGram found in db
                        // set "is_in_db_for_sure" to true for the whole predecessors subtree
                        dbkey_t cursor = ngram.predecessor;
                        for (size_t i = o - 2; i > 0; --i) {
                            ngram_t &predecessor = table[i][cursor];
                            predecessor.is_in_db_for_sure = true;
                            cursor = predecessor.predecessor;
                        }
                    }
                }
            }
        }
    }

    // Prepare write-batch
    // -------------------

    // Update n-grams (down to words)
    for (size_t o = order; o > 0; --o) {
        unordered_map<dbkey_t, ngram_t> &entry = table[o - 1];

        for (auto it = entry.begin(); it != entry.end(); ++it) {
            dbkey_t key = it->first;
            ngram_t &ngram = it->second;

            writeBatch.Merge(Slice(SerializeKey(domain, key)), Slice(SerializeCounts(ngram.counts)));
        }
    }

    // Store word counts
    counts_t wordCounts(wordCount, uniqueWordCount);
    writeBatch.Merge(Slice(SerializeKey(domain, WordCountsKey)), Slice(SerializeCounts(wordCounts)));

    return true;
}

void NGramStorage::ForceCompaction() {
    db->CompactRange(CompactRangeOptions(), NULL, NULL);
}

const vector<seqid_t> &NGramStorage::GetStreamsStatus() const {
    return streams;
}

StorageIterator *NGramStorage::NewIterator() {
    return new StorageIterator(db);
}

StorageIterator::StorageIterator(rocksdb::DB *db) {
    it = db->NewIterator(rocksdb::ReadOptions());
    it->SeekToFirst();
}

StorageIterator::~StorageIterator() {
    delete it;
}

bool StorageIterator::Next(domain_t *outDomain, dbkey_t *outKey, counts_t *outValue) {
    if (it->Valid()){
        Slice key = it->key();
        Slice value = it->value();

        DeserializeKey(key.data(), key.size(), outDomain, outKey);
        DeserializeCounts(value.data(), value.size(), outValue);

        it->Next();

        Status status = it->status();
        if (!status.ok())
            throw storage_exception(status.ToString());

        return true;
    } else {
        return false;
    }
}
