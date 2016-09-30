//
// Created by Davide  Caroselli on 28/09/16.
//

#ifndef SAPT_SUFFIXARRAY_H
#define SAPT_SUFFIXARRAY_H

#include <string>
#include <rocksdb/db.h>
#include <mmt/IncrementalModel.h>
#include <unordered_set>
#include <mutex>
#include "UpdateBatch.h"
#include "CorpusStorage.h"

using namespace std;

namespace mmt {
    namespace sapt {

        class index_exception : public exception {
        public:
            index_exception(const string &msg) : message(msg) {};

            virtual const char *what() const throw() override {
                return message.c_str();
            }

        private:
            string message;
        };

        struct sptr_t {
            int64_t offset;
            length_t sentence_offset;

            sptr_t(int64_t offset = 0, length_t sentence_offset = 0) : offset(offset),
                                                                       sentence_offset(sentence_offset) {};
        };

        struct sample_t {
            domain_t domain;
            vector<wid_t> source;
            vector<wid_t> target;
            alignment_t alignment;
            vector<length_t> offsets;
        };

        class SuffixArray {
        public:
            SuffixArray(const string &path, uint8_t prefixLength, uint8_t maxPhraseLength,
                        bool prepareForBulkLoad = false) throw(index_exception, storage_exception);

            ~SuffixArray();

            void GetRandomSamples(const vector<wid_t> &phrase, size_t limit, vector<sample_t> &outSamples,
                                  context_t *context);

            void GetRandomSamples(domain_t domain, const vector<wid_t> &phrase, size_t limit,
                                  vector<sample_t> &outSamples);

            size_t CountOccurrencesInSource(const vector<wid_t> &phrase);

            size_t CountOccurrencesInTarget(const vector<wid_t> &phrase);

            void PutBatch(UpdateBatch &batch) throw(index_exception, storage_exception);

            void ForceCompaction();

            const vector<seqid_t> &GetStreams() const {
                return streams;
            }

        private:
            const uint8_t prefixLength;
            const uint8_t maxPhraseLength;

            rocksdb::DB *db;
            CorpusStorage *storage;
            vector<seqid_t> streams;
            unordered_set<domain_t> domains;

            mutex domainsAccess;

            void AddPrefixesToBatch(domain_t domain, const vector<wid_t> &sentence, int64_t storageOffset,
                                    unordered_map<string, vector<sptr_t>> &outBatch);

            void AddOccurrencesToBatch(char type, const vector<wid_t> &phrase,
                                       unordered_map<string, size_t> &outBatch);

            void CollectSamples(domain_t domain, const vector<wid_t> &phrase, size_t offset, size_t length,
                                unordered_map<int64_t, vector<length_t>> &output);
        };

    }
}

#endif //SAPT_SUFFIXARRAY_H
