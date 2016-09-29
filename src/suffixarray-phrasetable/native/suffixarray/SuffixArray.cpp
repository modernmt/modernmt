//
// Created by Davide  Caroselli on 28/09/16.
//

#include "SuffixArray.h"
#include <random>
#include <algorithm>
#include <rocksdb/slice_transform.h>
#include <rocksdb/merge_operator.h>
#include <thread>
#include <util/ioutils.h>
#include <util/hashutils.h>
#include <boost/filesystem.hpp>
#include <iostream>

namespace fs = boost::filesystem;

using namespace rocksdb;
using namespace mmt;
using namespace mmt::sapt;

static_assert(sizeof(wid_t) == 4, "Current implementation works only with 32-bit word ids");
static_assert(sizeof(domain_t) == 4, "Current implementation works only with 32-bit domain id");
static_assert(sizeof(length_t) == 2, "Current implementation works only with 16-bit sentence length");

#define GlobalInfoKey() MakeKey(0, vector<wid_t>(), 0, 0)

static inline string MakeKey(domain_t domain, const vector<wid_t> &phrase, size_t offset, size_t length) {
    size_t size = sizeof(domain_t) + length * sizeof(wid_t);
    char *bytes = new char[size];

    size_t ptr = 0;

    WriteUInt32(bytes, &ptr, domain);
    for (size_t i = 0; i < length; ++i)
        WriteUInt32(bytes, &ptr, phrase[offset + i]);

    string key(bytes, size);
    delete[] bytes;

    return key;
}

static inline string MakePositionsList(const vector<sptr_t> &positions) {
    size_t size = positions.size() * (sizeof(int64_t) + sizeof(length_t));
    char *bytes = new char[size];

    size_t i = 0;
    for (auto position = positions.begin(); position != positions.end(); ++position) {
        WriteInt64(bytes, &i, position->offset);
        WriteUInt16(bytes, &i, position->sentence_offset);
    }

    string value(bytes, size);
    delete[] bytes;

    return value;
}

static inline void DeserializePositionsList(const char *data, size_t bytes_size,
                                            unordered_map<int64_t, vector<length_t>> &outPositions) {
    size_t entry_size = sizeof(int64_t) + sizeof(length_t);

    if (bytes_size % entry_size != 0)
        return;

    size_t count = bytes_size / entry_size;
    outPositions.reserve(outPositions.size() + count);

    size_t ptr = 0;
    for (size_t i = 0; i < count; ++i) {
        int64_t offset = ReadInt64(data, &ptr);
        length_t sentence_offset = ReadUInt16(data, &ptr);

        outPositions[offset].push_back(sentence_offset);
    }
}

static inline bool
DeserializeGlobalInfo(const char *data, size_t bytes_size, int64_t *outStorageSize, vector<seqid_t> *outStreams) {
    if (bytes_size < 8 || bytes_size % 8 != 0)
        return false;

    size_t ptr = 0;

    *outStorageSize = ReadInt64(data, &ptr);

    size_t size = (bytes_size - 8) / sizeof(seqid_t);
    outStreams->resize(size, 0);

    for (size_t i = 0; i < size; ++i)
        outStreams->at(i) = ReadUInt64(data, &ptr);

    return true;
}

static inline string SerializeGlobalInfo(const vector<seqid_t> &streams, int64_t storageSize) {
    size_t size = 8 + streams.size() * sizeof(seqid_t);
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

SuffixArray::SuffixArray(const string &modelPath, uint8_t prefixLength,
                         bool prepareForBulkLoad) throw(index_exception, storage_exception) :
        prefixLength(prefixLength) {
    fs::path modelDir(modelPath);

    if (!fs::is_directory(modelDir))
        throw invalid_argument("Invalid model path: " + modelPath);

    fs::path storageFile = fs::absolute(modelDir / fs::path("corpora.bin"));
    fs::path indexPath = fs::absolute(modelDir / fs::path("index"));

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

    Status status = DB::Open(options, indexPath.string(), &db);
    if (!status.ok())
        throw index_exception(status.ToString());

    db->CompactRange(CompactRangeOptions(), NULL, NULL);

    // Read streams
    string raw_streams;
    int64_t storageSize = 0;

    db->Get(ReadOptions(), GlobalInfoKey(), &raw_streams);
    DeserializeGlobalInfo(raw_streams.data(), raw_streams.size(), &storageSize, &streams);

    // Load storage
    storage = new CorpusStorage(storageFile.string(), storageSize);
}

SuffixArray::~SuffixArray() {
    delete db;
    delete storage;
}

void SuffixArray::ForceCompaction() {
    db->CompactRange(CompactRangeOptions(), NULL, NULL);
}

void SuffixArray::PutBatch(UpdateBatch &batch) throw(index_exception, storage_exception) {
    WriteBatch writeBatch;

    // Compute prefixes
    unordered_map<string, vector<sptr_t>> prefixes;

    for (auto entry = batch.data.begin(); entry != batch.data.end(); ++entry) {
        int64_t offset = storage->Append(entry->source, entry->target, entry->alignment);
        AddToBatch(entry->domain, entry->source, offset, prefixes);
    }

    int64_t storageSize = storage->Flush();

    // Add prefixes to write batch
    for (auto prefix = prefixes.begin(); prefix != prefixes.end(); ++prefix) {
        string value = MakePositionsList(prefix->second);
        writeBatch.Merge(prefix->first, value);
    }

    // Write global info
    writeBatch.Put(GlobalInfoKey(), SerializeGlobalInfo(batch.streams, storageSize));

    // Commit write batch
    Status status = db->Write(WriteOptions(), &writeBatch);
    if (!status.ok())
        throw index_exception("Unable to write to index: " + status.ToString());

    // Reset streams
    streams = batch.GetStreams();
}

void SuffixArray::AddToBatch(domain_t domain, const vector<wid_t> &sentence, int64_t storageOffset,
                             unordered_map<string, vector<sptr_t>> &outBatch) {
    for (length_t start = 0; start < sentence.size(); ++start) {
        size_t length = prefixLength;
        if (start + length > sentence.size())
            length = sentence.size() - start;

        string key = MakeKey(domain, sentence, start, length);
        outBatch[key].push_back(sptr_t(storageOffset, start));
    }
}

static void Retain(unordered_map<int64_t, vector<length_t>> &map,
                   const unordered_map<int64_t, vector<length_t>> &successors, length_t offset) {
    auto it = map.begin();
    while (it != map.end()) {
        bool remove;

        auto successor = successors.find(it->first);
        if (successor == successors.end()) {
            remove = true;
        } else {
            vector<length_t> &start_positions = it->second;
            const vector<length_t> &successors_offsets = successor->second;

            start_positions.erase(
                    remove_if(start_positions.begin(), start_positions.end(),
                              [successors_offsets, offset](length_t start) {
                                  if (offset > start)
                                      return false;

                                  auto e = find(successors_offsets.begin(), successors_offsets.end(), start - offset);
                                  return e != successors_offsets.end();
                              }),
                    start_positions.end()
            );

            remove = start_positions.empty();
        }

        if (remove)
            it = map.erase(it);
        else
            it++;
    }
}

void SuffixArray::GetRandomSamples(domain_t domain, const vector<wid_t> &phrase, size_t limit,
                                   vector<sample_t> &outSamples) {
    length_t phraseLength = (length_t) phrase.size();
    unordered_map<int64_t, vector<length_t>> positions;

    if (phraseLength <= prefixLength) {
        CollectSamples(domain, phrase, 0, phrase.size(), positions);
    } else {
        length_t start = 0;

        while (start < phraseLength) {
            if (start + prefixLength > phraseLength)
                start = phraseLength - prefixLength;

            unordered_map<int64_t, vector<length_t>> successors;
            CollectSamples(domain, phrase, start, prefixLength, successors);

            if (start == 0)
                positions = successors;
            else
                Retain(positions, successors, start);

            if (positions.empty())
                break;

            start += prefixLength;
        }
    }

    vector<int64_t> keys;
    keys.reserve(positions.size());

    for (auto &it : positions) {
        keys.push_back(it.first);
    }

    // Limit result
    if (limit > 0 && positions.size() > limit) {
        sort(keys.begin(), keys.end());

        unsigned int seed = 31 * words_hash(phrase) + domain;
        shuffle(keys.begin(), keys.end(), default_random_engine(seed));

        keys.resize(limit);
    }

    // Sort keys in order to minimize mmap jumps
    sort(keys.begin(), keys.end());

    // Resolve positions
    outSamples.resize(keys.size());
    size_t i = 0;

    for (auto key = keys.begin(); key != keys.end(); ++key) {
        sample_t &sample = outSamples[i++];
        sample.domain = domain;
        sample.offsets = positions[*key];

        storage->Retrieve(*key, &sample.source, &sample.target, &sample.alignment);
    }
}

void SuffixArray::CollectSamples(domain_t domain, const vector<wid_t> &phrase, size_t offset, size_t length,
                                 unordered_map<int64_t, vector<length_t>> &output) {
    string key = MakeKey(domain, phrase, offset, length);

    Iterator *it = db->NewIterator(ReadOptions());

    for (it->Seek(key); it->Valid() && it->key().starts_with(key); it->Next()) {
        Slice value = it->value();
        DeserializePositionsList(value.data_, value.size_, output);
    }

    delete it;
}