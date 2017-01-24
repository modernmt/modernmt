//
// Created by Davide Caroselli on 27/07/16.
//

#ifndef ILM_NGRAMSTORAGE_H
#define ILM_NGRAMSTORAGE_H

#include <string>
#include <vector>
#include <rocksdb/db.h>
#include <lm/LM.h>
#include <mmt/IncrementalModel.h>
#include "dbkey.h"
#include "counts.h"
#include "NGramBatch.h"

using namespace std;

namespace mmt {
    namespace ilm {

        class storage_exception : public exception {
        public:
            storage_exception(const string &msg) : message(msg) {};

            virtual const char *what() const throw() override {
                return message.c_str();
            }

        private:
            string message;
        };

        class NGramStorage {
        public:

            NGramStorage(string path, uint8_t order, bool prepareForBulkLoad = false) throw(storage_exception);

            ~NGramStorage();

            counts_t GetCounts(const domain_t domain, const dbkey_t key) const;

            void GetWordCounts(const domain_t domain, count_t *outUniqueWordCount, count_t *outWordCount) const;

            size_t GetEstimateSize() const;

            inline const uint8_t GetOrder() const {
                return order;
            }

            void PutBatch(NGramBatch &batch) throw(storage_exception);

            void ForceCompaction();

            const vector<seqid_t> &GetStreamsStatus() const;

            void Dump(string& dump_file);

        private:
            const uint8_t order;
            vector<seqid_t> streams;
            rocksdb::DB *db;
            rocksdb::Iterator* iterator;

            inline bool PrepareBatch(domain_t domain, ngram_table_t &table, rocksdb::WriteBatch &writeBatch);

            void ScanInit();
            void ScanTerminate();
            bool ScanNext(domain_t *domain, dbkey_t *key, counts_t *val);
        };

    }
}


#endif //ILM_NGRAMSTORAGE_H
