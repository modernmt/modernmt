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
#include "PostingList.h"

using namespace std;

namespace mmt {
    namespace sapt {

        extern const domain_t kBackgroundModelDomain;

        class index_exception : public exception {
        public:
            index_exception(const string &msg) : message(msg) {};

            virtual const char *what() const throw() override {
                return message.c_str();
            }

        private:
            string message;
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
            SuffixArray(const string &path, uint8_t prefixLength,
                        bool prepareForBulkLoad = false) throw(index_exception, storage_exception);

            ~SuffixArray();

            void GetRandomSamples(const vector<wid_t> &phrase, size_t limit, vector<sample_t> &outSamples,
                                  const context_t *context, bool searchInBackground = true);

            size_t CountOccurrences(bool isSource, const vector<wid_t> &phrase);

            void PutBatch(UpdateBatch &batch) throw(index_exception, storage_exception);

            void ForceCompaction();

            const vector<seqid_t> &GetStreams() const {
                return streams;
            }

        private:
            const uint8_t prefixLength;

            rocksdb::DB *db;
            CorpusStorage *storage;
            vector<seqid_t> streams;

            void AddPrefixesToBatch(bool isSource, domain_t domain, const vector<wid_t> &sentence,
                                    int64_t location, unordered_map<string, PostingList> &outBatch);

            void CollectLocations(bool isSource, domain_t domain, const vector<wid_t> &sentence,
                                  PostingList &output, unordered_set<int64_t> *coveredLocations = NULL);

            void CollectLocations(bool isSource, domain_t domain, const vector<wid_t> &phrase,
                                  size_t offset, size_t length, PostingList &output,
                                  const unordered_set<int64_t> *coveredLocations = NULL);

            void Retrieve(const map<int64_t, pair<domain_t, vector<length_t>>> &locations,
                          vector<sample_t> &outSamples);
        };

    }
}

#endif //SAPT_SUFFIXARRAY_H
