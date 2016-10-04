//
// Created by Davide  Caroselli on 28/09/16.
//

#include "SuffixArray.h"
#include "dbkv.h"
#include <rocksdb/slice_transform.h>
#include <rocksdb/merge_operator.h>
#include <boost/filesystem.hpp>
#include <thread>

namespace fs = boost::filesystem;

using namespace rocksdb;
using namespace mmt;
using namespace mmt::sapt;

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
        AddPrefixesToBatch(false, domain, entry->target, offset, targetPrefixes);
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
    size_t size = sentence.size();

    for (size_t start = 0; start < size; ++start) {
        for (size_t length = 1; length <= prefixLength; ++length) {
            if (start + length > size)
                break;

            string dkey = MakePrefixKey(prefixLength, isSource, domain, sentence, start, length);
            outBatch[dkey].Append(domain, location, (length_t) start);
        }
    }
}

/*
 * SuffixArray - Query
 */

size_t SuffixArray::CountOccurrences(bool isSource, const vector<wid_t> &phrase) {
    size_t count = 0;
    PrefixCursor *cursor = PrefixCursor::NewGlobalCursor(db, prefixLength, isSource);

    if (phrase.size() <= prefixLength) {
        for (cursor->Seek(phrase); cursor->HasNext(); cursor->Next())
            count += cursor->CountValue();
    } else {
        PostingList locations(phrase);
        CollectLocations(cursor, phrase, locations);

        count = locations.size();
    }

    delete cursor;
    return count;
}

void SuffixArray::GetRandomSamples(const vector<wid_t> &phrase, size_t limit, vector<sample_t> &outSamples,
                                   const context_t *context, bool searchInBackground) {
    // Get in-context samples
    samplemap_t inContextSamples;
    size_t inContextSize = 0;

    if (context && !context->empty()) {
        for (auto score = context->begin(); score != context->end(); ++score) {
            PostingList inContextPostingList(phrase);

            PrefixCursor *cursor = PrefixCursor::NewDomainCursor(db, prefixLength, true, score->domain);
            CollectLocations(cursor, phrase, inContextPostingList);
            delete cursor;

            if (limit == 0 || inContextSize + inContextPostingList.size() <= limit) {
                inContextPostingList.GetSamples(inContextSamples);
                inContextSize += inContextPostingList.size();
            } else {
                inContextPostingList.GetSamples(inContextSamples, limit - inContextSize);
                inContextSize = limit;
                break;
            }
        }
    }

    // Get out-context samples
    samplemap_t outContextSamples;

    if (searchInBackground && (limit == 0 || inContextSize < limit)) {
        PostingList outContextPostingList(phrase);

        PrefixCursor *cursor = PrefixCursor::NewGlobalCursor(db, prefixLength, true, context);
        CollectLocations(cursor, phrase, outContextPostingList);
        delete cursor;

        outContextPostingList.GetSamples(outContextSamples, limit == 0 ? 0 : limit - inContextSize);
    }

    outSamples.clear();

    if (inContextSamples.size() > 0)
        Retrieve(inContextSamples, outSamples);
    if (outContextSamples.size() > 0)
        Retrieve(outContextSamples, outSamples);
}

void SuffixArray::CollectLocations(PrefixCursor *cursor, const vector<wid_t> &sentence, PostingList &output) {
    length_t sentenceLength = (length_t) sentence.size();

    if (sentenceLength <= prefixLength) {
        CollectLocations(cursor, sentence, 0, sentence.size(), output);
    } else {
        length_t start = 0;
        PostingList collected(sentence);

        while (start < sentenceLength) {
            if (start + prefixLength > sentenceLength)
                start = sentenceLength - prefixLength;

            if (start == 0) {
                CollectLocations(cursor, sentence, start, prefixLength, collected);
            } else {
                PostingList successors(sentence, start, prefixLength);
                CollectLocations(cursor, sentence, start, prefixLength, successors);

                collected.Retain(successors, start);
            }

            if (collected.empty())
                break;

            start += prefixLength;
        }

        output.Join(collected);
    }
}

void SuffixArray::CollectLocations(PrefixCursor *cursor, const vector<wid_t> &phrase, size_t offset, size_t length,
                                   PostingList &output) {
    for (cursor->Seek(phrase, offset, length); cursor->HasNext(); cursor->Next()) {
        cursor->CollectValue(output);
    }
}

void SuffixArray::Retrieve(const samplemap_t &locations, vector<sample_t> &outSamples) {
    for (auto entry = locations.begin(); entry != locations.end(); ++entry) {
        outSamples.reserve(outSamples.size() + entry->second.size());

        domain_t domain = entry->first;
        for (auto location = entry->second.begin(); location != entry->second.end(); ++location) {
            sample_t sample;
            sample.domain = domain;
            sample.offsets = location->second;

            storage->Retrieve(location->first, &sample.source, &sample.target, &sample.alignment);

            outSamples.push_back(sample);
        }
    }
}