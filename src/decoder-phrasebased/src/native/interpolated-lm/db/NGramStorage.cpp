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
#include "dbkv.h"


const string kPathSeparator =
#ifdef _WIN32
        "\\";
#else
        "/";
#endif

using namespace std;
using namespace rocksdb;
using namespace mmt;
using namespace mmt::ilm;

static const ngram_hash_t kWordCountsHash = 0;
static const string kStreamsKey = MakeNGramKey(0, 0);

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

NGramStorage::NGramStorage(string basepath, uint8_t order, double gcTimeout,
                           bool prepareForBulkLoad) throw(storage_exception) : order(order),
                                                                               logger("ilm.NGramStorage") {
    rocksdb::Options options;
    options.create_if_missing = true;
    options.merge_operator.reset(new CountsAddOperator);

    PlainTableOptions plainTableOptions;
    plainTableOptions.user_key_len = sizeof(domain_t) + sizeof(ngram_hash_t);

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
    db->Get(ReadOptions(), kStreamsKey, &raw_streams);
    DeserializeStreams(raw_streams.data(), raw_streams.size(), &streams);

    // Garbage collector
    garbageCollector = new GarbageCollector(db, gcTimeout);
}

NGramStorage::~NGramStorage() {
    delete garbageCollector;
    delete db;
}

counts_t NGramStorage::GetCounts(const domain_t domain, const ngram_hash_t h) const {
    string key = MakeNGramKey(domain, h);
    string value;

    Status status = db->Get(ReadOptions(false, true), key, &value);

    if (!status.ok()) {
        if (status.IsNotFound())
            return counts_t();

        return counts_t();
    }

    counts_t output;
    return DeserializeCounts(value.data(), value.size(), &output) ? output : counts_t();
}

void NGramStorage::GetWordCounts(const domain_t domain, count_t *outUniqueWordCount, count_t *outWordCount) const {
    counts_t counts = GetCounts(domain, kWordCountsHash);
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
    LogInfo(logger) << "Importing batch of " << batch.sentenceCount << " sentences.";
    WriteBatch writeBatch;

    for (auto it = batch.ngrams_map.begin(); it != batch.ngrams_map.end(); ++it) {
        PrepareBatch(it->first, it->second, writeBatch);
    }

    // Write deleted domains
    for (auto domain = batch.deletions.begin(); domain != batch.deletions.end(); ++domain)
        writeBatch.Put(MakeDomainDeletionKey(*domain), "");

    // Store streams status
    writeBatch.Put(kStreamsKey, Slice(SerializeStreams(batch.GetStreams())));

    Status status = db->Write(WriteOptions(), &writeBatch);
    if (!status.ok())
        throw storage_exception(status.ToString());

    // Reset streams
    streams = batch.GetStreams();
    garbageCollector->MarkForDeletion(batch.deletions);
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
        unordered_map <ngram_hash_t, ngram_t> &entry = table[o - 1];

        for (auto it = entry.begin(); it != entry.end(); ++it) {
            ngram_hash_t h = it->first;
            ngram_t &ngram = it->second;

            if (o == 1)
                wordCount += ngram.counts.count;

            if (!ngram.is_in_db_for_sure) {
                string key = MakeNGramKey(domain, h);
                string value;
                status = db->Get(read_ops, key, &value);

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
                        ngram_hash_t cursor = ngram.predecessor;
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
        unordered_map <ngram_hash_t, ngram_t> &entry = table[o - 1];

        for (auto it = entry.begin(); it != entry.end(); ++it) {
            ngram_hash_t h = it->first;
            ngram_t &ngram = it->second;

            string key = MakeNGramKey(domain, h);
            writeBatch.Merge(key, SerializeCounts(ngram.counts));
        }
    }

    // Store word counts
    counts_t wordCounts(wordCount, uniqueWordCount);
    writeBatch.Merge(MakeNGramKey(domain, kWordCountsHash), SerializeCounts(wordCounts));

    return true;
}

void NGramStorage::ForceCompaction() {
    db->CompactRange(CompactRangeOptions(), NULL, NULL);
}

const vector <seqid_t> &NGramStorage::GetStreamsStatus() const {
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

bool StorageIterator::Next(domain_t *outDomain, ngram_hash_t *outKey, counts_t *outValue) {
    if (it->Valid()) {
        Slice key = it->key();
        Slice value = it->value();

        GetNGramKeyData(key.data(), key.size(), outDomain, outKey);
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
