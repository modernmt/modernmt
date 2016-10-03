//
// Created by Davide  Caroselli on 28/09/16.
//

#include "SuffixArray.h"
#include "dbkv.h"
#include <rocksdb/slice_transform.h>
#include <rocksdb/merge_operator.h>
#include <thread>
#include <boost/filesystem.hpp>
#include <iostream>
#include <util/hashutils.h>

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
    options.max_open_files = -1;
    options.compaction_style = kCompactionStyleLevel;

    if (prepareForBulkLoad) {
        options.PrepareForBulkLoad();
    } else {
        unsigned cpus = thread::hardware_concurrency();

        if (cpus > 1)
            options.IncreaseParallelism(cpus > 4 ? 4 : 2);

        options.level0_file_num_compaction_trigger = 8;
        options.level0_slowdown_writes_trigger = 17;
        options.level0_stop_writes_trigger = 24;
        options.num_levels = 4;

        options.write_buffer_size = 64L * 1024L * 1024L;
        options.max_write_buffer_number = 3;
        options.target_file_size_base = 64L * 1024L * 1024L;
        options.max_bytes_for_level_base = 512L * 1024L * 1024L;
        options.max_bytes_for_level_multiplier = 8;
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
    unordered_map<string, PostingList> sourcePrefixes;
    unordered_map<string, PostingList> targetPrefixes;

    for (auto entry = batch.data.begin(); entry != batch.data.end(); ++entry) {
        domain_t domain = entry->domain;

        int64_t offset = storage->Append(entry->source, entry->target, entry->alignment);
        AddPrefixesToBatch(true, domain, entry->source, offset, sourcePrefixes);
        AddPrefixesToBatch(false, kBackgroundModelDomain, entry->target, offset, targetPrefixes);
    }

    int64_t storageSize = storage->Flush();

    // Add prefixes to write batch
    for (auto prefix = sourcePrefixes.begin(); prefix != sourcePrefixes.end(); ++prefix) {
        string value = prefix->second.Serialize();
        writeBatch.Merge(prefix->first, value);
    }
    for (auto prefix = targetPrefixes.begin(); prefix != targetPrefixes.end(); ++prefix) {
        string value = prefix->second.Serialize();
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
                                     int64_t location, unordered_map<string, PostingList> &outBatch) {
    for (length_t start = 0; start < sentence.size(); ++start) {
        size_t length = prefixLength;
        if (start + length > sentence.size())
            length = sentence.size() - start;

        // Add to background model
        string key = MakePrefixKey(isSource, kBackgroundModelDomain, sentence, start, length);
        outBatch[key].Append(kBackgroundModelDomain, location, start);

        // Add to domain
        if (domain != kBackgroundModelDomain) {
            string dkey = MakePrefixKey(isSource, domain, sentence, start, length);
            outBatch[dkey].Append(domain, location, start);
        }
    }
}

/*
 * SuffixArray - Query
 */

size_t SuffixArray::CountOccurrences(bool isSource, const vector<wid_t> &phrase) {
    PostingList locations(phrase);
    CollectLocations(isSource, kBackgroundModelDomain, phrase, locations);

    return locations.size();
}

void SuffixArray::GetRandomSamples(const vector<wid_t> &phrase, size_t limit, vector<sample_t> &outSamples,
                                   const context_t *context, bool searchInBackground) {
    PostingList inContextLocations(phrase);
    PostingList outContextLocations(phrase);
    size_t remaining = limit;

    if (context) {
        for (auto score = context->begin(); score != context->end(); ++score) {
            CollectPositions(true, score->domain, phrase, inContextPositions);

            std::cerr << "Found " << outSamples.size()  << " samples" << std::endl;

            if (limit > 0) {
                if (inContextLocations.size() >= limit) {
                    remaining = 0;
                    break;
                } else {
                    remaining = limit - inContextLocations.size();
                }
            }
        }
    }

    if (searchInBackground && (limit == 0 || remaining > 0)) {
        unordered_set<int64_t> coveredLocations = inContextLocations.GetLocations();
        CollectLocations(true, kBackgroundModelDomain, phrase, outContextLocations, &coveredLocations);
    }

    outSamples.clear();

    ssize_t inContextSize = inContextLocations.size() - limit;
    if (inContextSize < 0) {
        map<int64_t, pair<domain_t, vector<length_t>>> inContext = inContextLocations.GetSamples();
        map<int64_t, pair<domain_t, vector<length_t>>> outContext =
                outContextLocations.GetSamples((size_t) -inContextSize, words_hash(phrase));

        Retrieve(inContext, outSamples);
        Retrieve(outContext, outSamples);
    } else {
        map<int64_t, pair<domain_t, vector<length_t>>> inContext = inContextLocations.GetSamples(limit);
        Retrieve(inContext, outSamples);
    }
}

void SuffixArray::CollectLocations(bool isSource, domain_t domain, const vector<wid_t> &sentence,
                                   PostingList &output, unordered_set<int64_t> *coveredLocations) {
    length_t sentenceLength = (length_t) sentence.size();

    if (sentenceLength <= prefixLength) {
        CollectLocations(isSource, domain, sentence, 0, sentence.size(), output, coveredLocations);
    } else {
        length_t start = 0;
        PostingList collected(sentence);

        while (start < sentenceLength) {
            if (start + prefixLength > sentenceLength)
                start = sentenceLength - prefixLength;

            if (start == 0) {
                CollectLocations(isSource, domain, sentence, start, prefixLength, collected, coveredLocations);
            } else {
                PostingList successors(sentence, start, prefixLength);
                CollectLocations(isSource, domain, sentence, start, prefixLength, successors, coveredLocations);

                collected.Retain(successors, start);
            }

            if (collected.empty())
                break;

            start += prefixLength;
        }

        output.Append(collected);
    }
}

void SuffixArray::CollectLocations(bool isSource, domain_t domain, const vector<wid_t> &phrase,
                                   size_t offset, size_t length, PostingList &output,
                                   const unordered_set<int64_t> *coveredLocations) {
    string key = MakePrefixKey(isSource, domain, phrase, offset, length);

    if (length == prefixLength) {
        string value;
        db->Get(ReadOptions(), key, &value);

        output.Append(domain, value.data(), value.size(), coveredLocations);
    } else {
        Iterator *it = db->NewIterator(ReadOptions());

        for (it->Seek(key); it->Valid() && it->key().starts_with(key); it->Next()) {
            Slice value = it->value();
            output.Append(domain, value.data_, value.size_, coveredLocations);
        }

        delete it;
    }
}

void
SuffixArray::Retrieve(const map<int64_t, pair<domain_t, vector<length_t>>> &locations, vector<sample_t> &outSamples) {
    // Resolve positions
    outSamples.reserve(outSamples.size() + locations.size());

    for (auto location = locations.begin(); location != locations.end(); ++location) {
        auto &value = location->second;

        sample_t sample;
        sample.domain = value.first;
        sample.offsets = value.second;

        storage->Retrieve(location->first, &sample.source, &sample.target, &sample.alignment);

        outSamples.push_back(sample);
    }
}
