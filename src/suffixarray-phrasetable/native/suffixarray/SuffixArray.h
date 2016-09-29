//
// Created by Davide  Caroselli on 28/09/16.
//

#ifndef SAPT_SUFFIXARRAY_H
#define SAPT_SUFFIXARRAY_H

#include <string>
#include <rocksdb/db.h>
#include <mmt/IncrementalModel.h>
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
            SuffixArray(const string &path, uint8_t prefixLength,
                        bool prepareForBulkLoad = false) throw(index_exception, storage_exception);

            ~SuffixArray();

            void GetRandomSamples(domain_t domain, const vector<wid_t> &phrase, size_t limit,
                                  vector<sample_t> &outSamples);

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

            void AddToBatch(domain_t domain, const vector<wid_t> &sentence, int64_t storageOffset,
                            unordered_map<string, vector<sptr_t>> &outBatch);

            void CollectSamples(domain_t domain, const vector<wid_t> &phrase, size_t offset, size_t length,
                                unordered_map<int64_t, vector<length_t>> &output);
        };

    }
}

#endif //SAPT_SUFFIXARRAY_H
