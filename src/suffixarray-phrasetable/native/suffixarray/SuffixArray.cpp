//
// Created by Davide  Caroselli on 28/09/16.
//

#include "SuffixArray.h"
#include "dbkv.h"
#include <random>
#include <algorithm>
#include <rocksdb/slice_transform.h>
#include <rocksdb/merge_operator.h>
#include <thread>
#include <boost/filesystem.hpp>
#include <iostream>
#include <util/chrono.h>

namespace fs = boost::filesystem;

using namespace rocksdb;
using namespace mmt;
using namespace mmt::sapt;

const domain_t mmt::sapt::kBackgroundModelDomain = 0;
static const string kGlobalInfoKey = MakeEmptyKey(kGlobalInfoKeyType);

/*
 * MergePositionOperator
 */

namespace mmt {
    namespace sapt {

        class MergePositionOperator : public AssociativeMergeOperator {
        public:
            virtual bool Merge(const Slice &key, const Slice *existing_value, const Slice &value, string *new_value,
                               Logger *logger) const override {
                switch (key.data_[0]) {
                    case kSourcePrefixKeyType:
                    case kTargetPrefixKeyType:
                        MergePositionLists(existing_value, value, new_value);
                        return true;
                    default:
                        return false;
                }
            }

            inline void MergePositionLists(const Slice *existing_value, const Slice &value, string *new_value) const {
                if (existing_value)
                    *new_value = existing_value->ToString() + value.ToString();
                else
                    *new_value = value.ToString();
            }

            virtual const char *Name() const override {
                return "MergePositionOperator";
            }
        };

    }
}

/*
 * SuffixArray - Initialization
 */

SuffixArray::SuffixArray(const string &modelPath, uint8_t prefixLength, uint8_t maxPhraseLength,
                         bool prepareForBulkLoad) throw(index_exception, storage_exception) :
        prefixLength(prefixLength), maxPhraseLength(maxPhraseLength) {
    fs::path modelDir(modelPath);

    if (!fs::is_directory(modelDir))
        throw invalid_argument("Invalid model path: " + modelPath);

    fs::path storageFile = fs::absolute(modelDir / fs::path("corpora.bin"));
    fs::path indexPath = fs::absolute(modelDir / fs::path("index"));

    rocksdb::Options options;
    options.create_if_missing = true;
    options.merge_operator.reset(new MergePositionOperator);
    options.prefix_extractor.reset(NewNoopTransform());
    options.max_open_files = -1;
    options.compaction_style = kCompactionStyleLevel;

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

    db->Get(ReadOptions(), kGlobalInfoKey, &raw_streams);
    DeserializeGlobalInfo(raw_streams.data(), raw_streams.size(), &storageSize, &streams);

    // Load storage
    storage = new CorpusStorage(storageFile.string(), storageSize);
}

SuffixArray::~SuffixArray() {
    delete db;
    delete storage;
}

/*
 * SuffixArray - Indexing
 */

void SuffixArray::ForceCompaction() {
    db->CompactRange(CompactRangeOptions(), NULL, NULL);
}

void SuffixArray::PutBatch(UpdateBatch &batch) throw(index_exception, storage_exception) {
    WriteBatch writeBatch;

    // Compute prefixes
    unordered_map<string, vector<sptr_t>> sourcePrefixes;
    unordered_map<string, vector<sptr_t>> targetPrefixes;

    for (auto entry = batch.data.begin(); entry != batch.data.end(); ++entry) {
        domain_t domain = entry->domain;

        int64_t offset = storage->Append(entry->source, entry->target, entry->alignment);
        AddPrefixesToBatch(true, domain, entry->source, offset, sourcePrefixes);
        AddPrefixesToBatch(false, kBackgroundModelDomain, entry->target, offset, targetPrefixes);
    }

    int64_t storageSize = storage->Flush();

    // Add prefixes to write batch
    for (auto prefix = sourcePrefixes.begin(); prefix != sourcePrefixes.end(); ++prefix) {
        string value = SerializePositionsList(prefix->second);
        writeBatch.Merge(prefix->first, value);
    }
    for (auto prefix = targetPrefixes.begin(); prefix != targetPrefixes.end(); ++prefix) {
        string value = SerializePositionsList(prefix->second);
        writeBatch.Merge(prefix->first, value);
    }

    // Write global info
    writeBatch.Put(kGlobalInfoKey, SerializeGlobalInfo(batch.streams, storageSize));

    // Commit write batch
    Status status = db->Write(WriteOptions(), &writeBatch);
    if (!status.ok())
        throw index_exception("Unable to write to index: " + status.ToString());

    // Reset streams and domains
    streams = batch.GetStreams();
}

void SuffixArray::AddPrefixesToBatch(bool isSource, domain_t domain, const vector<wid_t> &sentence,
                                     int64_t storageOffset, unordered_map<string, vector<sptr_t>> &outBatch) {
    for (length_t start = 0; start < sentence.size(); ++start) {
        size_t length = prefixLength;
        if (start + length > sentence.size())
            length = sentence.size() - start;

        // Add to background model
        string key = MakePrefixKey(isSource, kBackgroundModelDomain, sentence, start, length);
        outBatch[key].push_back(sptr_t(storageOffset, start));

        // Add to domain
        if (domain != kBackgroundModelDomain) {
            string dkey = MakePrefixKey(isSource, domain, sentence, start, length);
            outBatch[dkey].push_back(sptr_t(storageOffset, start));
        }
    }
}

/*
 * SuffixArray - Query
 */

size_t SuffixArray::CountOccurrences(bool isSource, const vector<wid_t> &phrase) {
    positionsmap_t positions;
    CollectPositions(isSource, kBackgroundModelDomain, phrase, positions);

    size_t size = 0;
    for (auto entry = positions.begin(); entry != positions.end(); ++entry) {
        size += entry->second.second.size();
    }

    return size;
}

void SuffixArray::GetRandomSamples(const vector<wid_t> &phrase, size_t limit, vector<sample_t> &outSamples,
                                   context_t *context) {
    double begin = GetTime();
    double globalBegin = begin;
    positionsmap_t inContextPositions;
    positionsmap_t outContextPositions;
    size_t remaining = limit;

    if (context) {
        for (auto score = context->begin(); score != context->end(); ++score) {
            CollectPositions(true, score->domain, phrase, inContextPositions);

            if (limit > 0) {
                if (inContextPositions.size() >= limit) {
                    remaining = 0;
                    break;
                } else {
                    remaining = limit - inContextPositions.size();
                }
            }
        }
    }
    cerr << "SuffixArray::GetRandomSamples from context took " << GetElapsedTime(begin) << "s" << endl;

    begin = GetTime();

    if (limit == 0 || remaining > 0)
        CollectPositions(true, kBackgroundModelDomain, phrase, outContextPositions, &inContextPositions);

    cerr << "SuffixArray::GetRandomSamples from background took " << GetElapsedTime(begin) << "s" << endl;

    outSamples.clear();

    ssize_t inContextSize = inContextPositions.size() - limit;
    if (inContextSize < 0) {
        Retrieve(inContextPositions, outSamples, 0);
        Retrieve(outContextPositions, outSamples, (size_t) -inContextSize);
    } else {
        Retrieve(inContextPositions, outSamples, limit);
    }

    cerr << "SuffixArray::GetRandomSamples took " << GetElapsedTime(globalBegin) << "s" << endl;
}

static void Retain(positionsmap_t &map, const positionsmap_t &successors, length_t offset) {
    auto it = map.begin();
    while (it != map.end()) {
        bool remove;

        auto successor = successors.find(it->first);
        if (successor == successors.end()) {
            remove = true;
        } else {
            vector<length_t> &start_positions = it->second.second;
            const vector<length_t> &successors_offsets = successor->second.second;

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

void SuffixArray::CollectPositions(bool isSource, domain_t domain, const vector<wid_t> &sentence,
                                   positionsmap_t &outPositions, positionsmap_t *coveredPositions) {
    length_t sentenceLength = (length_t) sentence.size();

    if (sentenceLength <= prefixLength) {
        CollectPositions(isSource, domain, sentence, 0, sentence.size(), outPositions, coveredPositions);
    } else {
        length_t start = 0;
        positionsmap_t collected;

        while (start < sentenceLength) {
            if (start + prefixLength > sentenceLength)
                start = sentenceLength - prefixLength;

            positionsmap_t successors;
            CollectPositions(isSource, domain, sentence, start, prefixLength, successors, coveredPositions);

            if (start == 0)
                collected = successors;
            else
                Retain(collected, successors, start);

            if (collected.empty())
                break;

            start += prefixLength;
        }

        outPositions.reserve(outPositions.size() + collected.size());
        outPositions.insert(collected.begin(), collected.end());
    }
}

void SuffixArray::CollectPositions(bool isSource, domain_t domain, const vector<wid_t> &phrase,
                                   size_t offset, size_t length, positionsmap_t &output,
                                   positionsmap_t *coveredPositions) {
    double begin = GetTime();
    int count = 0;

    string key = MakePrefixKey(isSource, domain, phrase, offset, length);

    Iterator *it = db->NewIterator(ReadOptions());

    for (it->Seek(key); it->Valid() && it->key().starts_with(key); it->Next()) {
        count++;
        Slice value = it->value();
        DeserializePositionsList(domain, value.data_, value.size_, output, coveredPositions);
    }

    cerr << "SuffixArray::CollectPositions took " << GetElapsedTime(begin) << "s for " << count << " prefixes" << endl;

    delete it;
}

void SuffixArray::Retrieve(const positionsmap_t &positions, vector<sample_t> &outSamples, size_t limit) {
    if (positions.empty())
        return;

    double begin = GetTime();
    vector<int64_t> keys;
    keys.reserve(positions.size());

    for (auto &it : positions) {
        keys.push_back(it.first);
    }
    cerr << "SuffixArray::Retrieve collect keys took " << GetElapsedTime(begin) << "s" << endl;

    begin = GetTime();
    // Limit result
    if (limit > 0 && positions.size() > limit) {
        sort(keys.begin(), keys.end());

        unsigned int seed = 3874556238;
        shuffle(keys.begin(), keys.end(), default_random_engine(seed));

        keys.resize(limit);
    }

    cerr << "SuffixArray::Retrieve limit results took " << GetElapsedTime(begin) << "s" << endl;

    begin = GetTime();
    // Sort keys in order to minimize mmap jumps
    sort(keys.begin(), keys.end());

    // Resolve positions
    outSamples.reserve(outSamples.size() + keys.size());

    for (auto key = keys.begin(); key != keys.end(); ++key) {
        auto &value = positions.find(*key)->second;

        sample_t sample;
        sample.domain = value.first;
        sample.offsets = value.second;

        storage->Retrieve(*key, &sample.source, &sample.target, &sample.alignment);

        outSamples.push_back(sample);
    }

    cerr << "SuffixArray::Retrieve lookup took " << GetElapsedTime(begin) << "s" << endl;
}
