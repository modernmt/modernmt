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

const static char kGlobalInfoKeyType = 0;
const static char kDomainListKeyType = 1;
const static char kPrefixKeyType = 2;
const static char kSourceOccurrencesKeyType = 3;
const static char kTargetOccurrencesKeyType = 4;

/* Keys */

static inline string MakeEmptyKey(char type) {
    char bytes[1];
    bytes[0] = type;
    string key(bytes, 1);
    return key;
}

static inline string MakePrefixKey(domain_t domain, const vector<wid_t> &phrase, size_t offset, size_t length) {
    size_t size = 1 + sizeof(domain_t) + length * sizeof(wid_t);
    char *bytes = new char[size];
    bytes[0] = kPrefixKeyType;

    size_t ptr = 1;

    WriteUInt32(bytes, &ptr, domain);
    for (size_t i = 0; i < length; ++i)
        WriteUInt32(bytes, &ptr, phrase[offset + i]);

    string key(bytes, size);
    delete[] bytes;

    return key;
}

static inline string MakeOccurrencesKey(char type, const vector<wid_t> &phrase, size_t offset, size_t length) {
    uint64_t hash = 0;

    for (size_t i = 0; i < length; ++i) {
        wid_t word = phrase[offset + i];

        if (hash == 0)
            hash = word;
        else
            hash = ((hash * 8978948897894561157ULL) ^ (static_cast<uint64_t>(1 + word) * 17894857484156487943ULL));
    }

    char bytes[9];
    bytes[0] = type;

    size_t ptr = 1;
    WriteUInt64(bytes, &ptr, hash);

    return string(bytes, 9);
}

/* Values */

static inline string SerializePositionsList(const vector<sptr_t> &positions) {
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

static inline string SerializeDomainList(const unordered_set<domain_t> &domains) {
    size_t size = domains.size() * sizeof(domain_t);
    char *bytes = new char[size];

    size_t i = 0;
    for (auto domain = domains.begin(); domain != domains.end(); ++domain)
        WriteUInt32(bytes, &i, *domain);

    string value(bytes, size);
    delete[] bytes;

    return value;
}

static inline void DeserializeDomainList(const char *data, size_t bytes_size, unordered_set<domain_t> &outDomains) {
    size_t entry_size = sizeof(domain_t);

    if (bytes_size % entry_size != 0)
        return;

    size_t count = bytes_size / entry_size;
    outDomains.reserve(count);

    size_t ptr = 0;
    for (size_t i = 0; i < count; ++i)
        outDomains.insert(ReadUInt32(data, &ptr));
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

static inline string SerializeCount(size_t count) {
    char bytes[8];
    size_t ptr = 0;
    WriteUInt64(bytes, &ptr, (uint64_t) count);

    return string(bytes, 8);
}

static inline size_t DeserializeCount(const char *data, size_t bytes_size) {
    if (bytes_size != 8)
        return 0;

    size_t ptr = 0;
    return (size_t) ReadUInt64(data, &ptr);
}

/* Merge operator implementation */

namespace mmt {
    namespace sapt {

        class MergePositionOperator : public AssociativeMergeOperator {
        public:
            virtual bool Merge(const Slice &key, const Slice *existing_value, const Slice &value, string *new_value,
                               Logger *logger) const override {
                switch (key.data_[0]) {
                    case kPrefixKeyType:
                        MergePositionLists(existing_value, value, new_value);
                        break;
                    case kSourceOccurrencesKeyType:
                    case kTargetOccurrencesKeyType:
                        MergeCounts(existing_value, value, new_value);
                        break;
                    default:
                        return false;
                }

                return true;
            }

            void MergePositionLists(const Slice *existing_value, const Slice &value, string *new_value) const {
                if (existing_value) {
                    *new_value = existing_value->ToString() + value.ToString();
                } else {
                    *new_value = value.ToString();
                }
            }

            void MergeCounts(const Slice *existing_value, const Slice &value, string *new_value) const {
                uint64_t existing = 0;
                if (existing_value)
                    existing = DeserializeCount(existing_value->data_, existing_value->size_);

                uint64_t update = DeserializeCount(value.data_, value.size_);
                *new_value = SerializeCount(existing + update);
            }

            virtual const char *Name() const override {
                return "MergePositionOperator";
            }
        };

    }
}

/* SuffixArray implementation */

static const string kGlobalInfoKey = MakeEmptyKey(kGlobalInfoKeyType);
static const string kDomainListKey = MakeEmptyKey(kDomainListKeyType);

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

    db->Get(ReadOptions(), kGlobalInfoKey, &raw_streams);
    DeserializeGlobalInfo(raw_streams.data(), raw_streams.size(), &storageSize, &streams);

    // Read domains
    string raw_domains;
    db->Get(ReadOptions(), kDomainListKey, &raw_domains);
    DeserializeDomainList(raw_domains.data(), raw_domains.size(), domains);

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

    unordered_set<domain_t> new_domains;

    // Compute prefixes
    unordered_map<string, vector<sptr_t>> prefixes;
    unordered_map<string, size_t> sourceOccurrences;
    unordered_map<string, size_t> targetOccurrences;

    for (auto entry = batch.data.begin(); entry != batch.data.end(); ++entry) {
        domain_t domain = entry->domain;

        int64_t offset = storage->Append(entry->source, entry->target, entry->alignment);
        AddPrefixesToBatch(domain, entry->source, offset, prefixes);
        AddOccurrencesToBatch(kSourceOccurrencesKeyType, entry->source, sourceOccurrences);
        AddOccurrencesToBatch(kTargetOccurrencesKeyType, entry->target, targetOccurrences);

        if (new_domains.find(domain) == new_domains.end() && domains.find(domain) == domains.end())
            new_domains.insert(domain);
    }

    int64_t storageSize = storage->Flush();

    // Add prefixes to write batch
    for (auto prefix = prefixes.begin(); prefix != prefixes.end(); ++prefix) {
        string value = SerializePositionsList(prefix->second);
        writeBatch.Merge(prefix->first, value);
    }

    // Add occurrences to write batch
    for (auto occurrences = sourceOccurrences.begin(); occurrences != sourceOccurrences.end(); ++occurrences) {
        string value = SerializeCount(occurrences->second);
        writeBatch.Merge(occurrences->first, value);
    }
    for (auto occurrences = targetOccurrences.begin(); occurrences != targetOccurrences.end(); ++occurrences) {
        string value = SerializeCount(occurrences->second);
        writeBatch.Merge(occurrences->first, value);
    }

    // Write global info
    writeBatch.Put(kGlobalInfoKey, SerializeGlobalInfo(batch.streams, storageSize));

    // Write domain list if necessary
    if (!new_domains.empty()) {
        new_domains.insert(domains.begin(), domains.end());
        writeBatch.Put(kDomainListKey, SerializeDomainList(new_domains));
    }

    // Commit write batch
    Status status = db->Write(WriteOptions(), &writeBatch);
    if (!status.ok())
        throw index_exception("Unable to write to index: " + status.ToString());

    // Reset streams and domains
    streams = batch.GetStreams();
    if (!new_domains.empty()) {
        domainsAccess.lock();
        domains = new_domains;
        domainsAccess.unlock();
    }
}

void SuffixArray::AddPrefixesToBatch(domain_t domain, const vector<wid_t> &sentence, int64_t storageOffset,
                                     unordered_map<string, vector<sptr_t>> &outBatch) {
    for (length_t start = 0; start < sentence.size(); ++start) {
        size_t length = prefixLength;
        if (start + length > sentence.size())
            length = sentence.size() - start;

        string key = MakePrefixKey(domain, sentence, start, length);
        outBatch[key].push_back(sptr_t(storageOffset, start));
    }
}

void SuffixArray::AddOccurrencesToBatch(char type, const vector<wid_t> &sentence,
                                        unordered_map<string, size_t> &outBatch) {
    for (size_t iword = 0; iword < sentence.size(); ++iword) {
        for (size_t iorder = 0; iorder < maxPhraseLength; ++iorder) {
            if (iword + iorder >= sentence.size())
                break;

            string key = MakeOccurrencesKey(type, sentence, iword, iorder + 1);
            outBatch[key]++;
        }
    }
}

void SuffixArray::GetRandomSamples(const vector<wid_t> &phrase, size_t limit, vector<sample_t> &outSamples,
                                   context_t *context) {
    size_t remaining = limit;
    outSamples.clear();

    unordered_set<domain_t> covered_domains;

    if (context) {
        for (auto score = context->begin(); score != context->end(); ++score) {

            cout << "domain: " << score->domain << " score: " << score->score << " remaining:" << remaining << endl;
            covered_domains.insert(score->domain);

            GetRandomSamples(score->domain, phrase, remaining, outSamples);

            std::cerr << "Found " << outSamples.size()  << " samples" << std::endl;

            if (limit > 0) {
                if (outSamples.size() >= limit) {
                    remaining = 0;
                    break;
                } else {
                    remaining = limit - outSamples.size();
                }
            }
        }
    }

    if (limit == 0 || remaining > 0) {
        vector<domain_t> domain_sequence;

        domainsAccess.lock();
        for (auto domain = domains.begin(); domain != domains.end(); ++domain) {
            if (covered_domains.find(*domain) == covered_domains.end())
                domain_sequence.push_back(*domain);
        }
        domainsAccess.unlock();

        sort(domain_sequence.begin(), domain_sequence.end());

        unsigned int seed = words_hash(phrase);
        shuffle(domain_sequence.begin(), domain_sequence.end(), default_random_engine(seed));

        for (auto domain = domain_sequence.begin(); domain != domain_sequence.end(); ++domain) {
            GetRandomSamples(*domain, phrase, remaining, outSamples);

            if (limit > 0) {
                if (outSamples.size() >= limit) {
                    break;
                } else {
                    remaining = limit - outSamples.size();
                }
            }
        }
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
    string key = MakePrefixKey(domain, phrase, offset, length);

    Iterator *it = db->NewIterator(ReadOptions());

    for (it->Seek(key); it->Valid() && it->key().starts_with(key); it->Next()) {
        Slice value = it->value();
        DeserializePositionsList(value.data_, value.size_, output);
    }

    delete it;
}

size_t SuffixArray::CountOccurrencesInSource(const vector<wid_t> &phrase) {
    if (phrase.size() > maxPhraseLength)
        return 0;

    string key = MakeOccurrencesKey(kSourceOccurrencesKeyType, phrase, 0, phrase.size());
    string value;

    db->Get(ReadOptions(), key, &value);
    return DeserializeCount(value.data(), value.size());
}

size_t SuffixArray::CountOccurrencesInTarget(const vector<wid_t> &phrase) {
    if (phrase.size() > maxPhraseLength)
        return 0;

    string key = MakeOccurrencesKey(kTargetOccurrencesKeyType, phrase, 0, phrase.size());
    string value;

    db->Get(ReadOptions(), key, &value);
    return DeserializeCount(value.data(), value.size());
}