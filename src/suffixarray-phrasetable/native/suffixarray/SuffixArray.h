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
#include <mmt/sentence.h>
#include "UpdateBatch.h"
#include "CorpusStorage.h"
#include "PostingList.h"
#include "PrefixCursor.h"
#include "Collector.h"
#include "sample.h"

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

        class SuffixArray {
        public:
            SuffixArray(const string &path, uint8_t prefixLength,
                        bool prepareForBulkLoad = false) throw(index_exception, storage_exception);

            ~SuffixArray();

            void GetRandomSamples(const vector<wid_t> &phrase, size_t limit, vector<sample_t> &outSamples,
                                  const context_t *context = NULL, bool searchInBackground = true);

            Collector *NewCollector(const context_t *context = NULL, bool searchInBackground = true);

            size_t CountOccurrences(bool isSource, const vector<wid_t> &phrase);

            void PutBatch(UpdateBatch &batch) throw(index_exception, storage_exception);

            void ForceCompaction() throw(index_exception);

            const vector<seqid_t> &GetStreams() const {
                return streams;
            }

            void Dump(string& dump_file);

        private:
            const bool openForBulkLoad;
            const uint8_t prefixLength;

            rocksdb::DB *db;
            rocksdb::Iterator* iterator;
            CorpusStorage *storage;
            vector<seqid_t> streams;

            void AddPrefixesToBatch(domain_t domain, const vector<wid_t> &sentence,
                                    int64_t location, unordered_map<string, PostingList> &outBatch);

            void AddTargetCountsToBatch(const vector<wid_t> &sentence, unordered_map<string, uint64_t> &outBatch);

            void ScanInit();
            void ScanTerminate();
            bool ScanNext(string& key, string& value);
        };

    }
}

#endif //SAPT_SUFFIXARRAY_H
