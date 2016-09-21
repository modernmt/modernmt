//
// Created by Davide Caroselli on 27/07/16.
//

#ifndef ROCKSLM_NGRAMSTORAGE_H
#define ROCKSLM_NGRAMSTORAGE_H

#include <string>
#include <vector>
#include <rocksdb/db.h>
#include <lm/LM.h>
#include <mmt/IncrementalModel.h>
#include "dbkey.h"
#include "counts.h"
#include "NGramBatch.h"

using namespace std;
using namespace mmt;

namespace rockslm {
    namespace db {

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

        private:
            const uint8_t order;
            vector<seqid_t> streams;
            rocksdb::DB *db;

            inline bool PrepareBatch(domain_t domain, ngram_table_t &table, rocksdb::WriteBatch &writeBatch);
        };

    }
}


#endif //ROCKSLM_NGRAMSTORAGE_H
