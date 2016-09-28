//
// Created by Davide  Caroselli on 27/09/16.
//

#include "CorpusIndex.h"
#include <rocksdb/slice_transform.h>
#include <rocksdb/merge_operator.h>
#include <thread>
#include <util/ioutils.h>

using namespace rocksdb;
using namespace mmt::sapt;

static_assert(sizeof(mmt::wid_t) == 4, "Current implementation works only with 32-bit word ids");
static_assert(sizeof(mmt::domain_t) == 4, "Current implementation works only with 32-bit domain id");
static_assert(sizeof(mmt::length_t) == 2, "Current implementation works only with 16-bit sentence length");

#define GlobalInfoKey() MakeKey(0, vector<wid_t>())

static inline string MakeKey(mmt::domain_t domain, const vector<mmt::wid_t> &prefix) {
    size_t size = sizeof(mmt::domain_t) + prefix.size() * sizeof(mmt::wid_t);
    char *bytes = new char[size];

    size_t ki = 0;

    WriteUInt32(bytes, &ki, domain);

    for (size_t i = 0; i < prefix.size(); ++i)
        WriteUInt32(bytes, &ki, prefix[i]);

    string key(bytes, size);
    delete[] bytes;

    return key;
}

static inline string MakePositionsList(int64_t offset, const vector<mmt::sapt::position_t> &positions) {
    size_t size = positions.size() * (sizeof(int64_t) + sizeof(mmt::length_t));
    char *bytes = new char[size];

    size_t i = 0;
    for (auto position = positions.begin(); position != positions.end(); ++position) {
        WriteInt64(bytes, &i, offset + position->corpus_offset);
        WriteUInt16(bytes, &i, position->word_offset);
    }

    string value(bytes, size);
    delete[] bytes;

    return value;
}

static inline bool
DeserializeGlobalInfo(const char *data, size_t bytes_size, int64_t *outStorageSize, vector<mmt::seqid_t> *outStreams) {
    if (bytes_size < 8 || bytes_size % 8 != 0)
        return false;

    size_t ptr = 0;

    *outStorageSize = ReadInt64(data, &ptr);

    size_t size = (bytes_size - 8) / sizeof(mmt::seqid_t);
    outStreams->resize(size, 0);

    for (size_t i = 0; i < size; ++i)
        outStreams->at(i) = ReadUInt64(data, &ptr);

    return true;
}

static inline string SerializeGlobalInfo(const vector<mmt::seqid_t> &streams, int64_t storageSize) {
    size_t size = 8 + streams.size() * sizeof(mmt::seqid_t);
    char *bytes = new char[size];
    size_t i = 0;

    WriteInt64(bytes, &i, storageSize);
    for (auto id = streams.begin(); id != streams.end(); ++id)
        WriteUInt64(bytes, &i, *id);

    string result = string(bytes, size);
    delete[] bytes;

    return result;
}

namespace mmt {
    namespace sapt {

        class MergePositionOperator : public AssociativeMergeOperator {
        public:
            virtual bool Merge(const Slice &key, const Slice *existing_value, const Slice &value, string *new_value,
                               Logger *logger) const override {
                if (existing_value) {
                    *new_value = existing_value->ToString() + value.ToString();
                } else {
                    *new_value = value.ToString();
                }

                return true;
            }

            virtual const char *Name() const override {
                return "MergePositionOperator";
            }
        };

    }
}

CorpusIndex::CorpusIndex(const string &path, uint8_t prefixLength, bool prepareForBulkLoad) throw(index_exception)
        : prefixLength(prefixLength), storageSize(0) {
    rocksdb::Options options;
    options.create_if_missing = true;
    options.merge_operator.reset(new MergePositionOperator);

//    PlainTableOptions plainTableOptions;
//    plainTableOptions.user_key_len = 1 + sizeof(domain_t) + kMaxInternalPhraseLength * sizeof(wid_t);

    options.prefix_extractor.reset(NewNoopTransform());
//    options.prefix_extractor.reset(NewFixedPrefixTransform(1 + sizeof(domain_t) + sizeof(wid_t)));
//    options.table_factory.reset(NewPlainTableFactory(plainTableOptions));
//    options.memtable_factory.reset(NewHashLinkListRepFactory());
//    options.allow_mmap_reads = true;
//
    options.max_open_files = -1;
    options.compaction_style = kCompactionStyleLevel;
//    options.memtable_prefix_bloom_bits = 1024 * 1024 * 8;
//
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

    Status status = DB::Open(options, path, &db);
    if (!status.ok())
        throw index_exception(status.ToString());

    db->CompactRange(CompactRangeOptions(), NULL, NULL);

    // Read streams
    string raw_streams;
    db->Get(ReadOptions(), GlobalInfoKey(), &raw_streams);
    DeserializeGlobalInfo(raw_streams.data(), raw_streams.size(), &storageSize, &streams);
}

CorpusIndex::~CorpusIndex() {
    delete db;
}

void CorpusIndex::ForceCompaction() {
    db->CompactRange(CompactRangeOptions(), NULL, NULL);
}

void CorpusIndex::PutBatch(UpdateBatch &batch) throw(index_exception) {
    WriteBatch writeBatch;

    // Write prefixes
    int64_t baseOffset = batch.baseOffset;
    for (auto prefixes = batch.prefixes.begin(); prefixes != batch.prefixes.end(); ++prefixes) {
        domain_t domain = prefixes->first;

        for (auto prefix = prefixes->second.begin(); prefix != prefixes->second.end(); ++prefix) {
            string key = MakeKey(domain, prefix->first);
            string value = MakePositionsList(baseOffset, prefix->second);

            writeBatch.Merge(key, value);
        }
    }

    // Write global info
    writeBatch.Put(GlobalInfoKey(), SerializeGlobalInfo(batch.streams, batch.storageSize));

    // Commit write batch
    Status status = db->Write(WriteOptions(), &writeBatch);
    if (!status.ok())
        throw index_exception("Unable to write to index: " + status.ToString());

    // Reset streams
    streams = batch.GetStreams();
    storageSize = batch.storageSize;
}
