//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_CORPUSINDEX_H
#define SAPT_CORPUSINDEX_H

#include <string>
#include <rocksdb/db.h>
#include <mmt/IncrementalModel.h>
#include <sapt/position.h>
#include "UpdateBatch.h"

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

        class CorpusIndex {
        public:
            CorpusIndex(const string &path, uint8_t prefixLength,
                        bool prepareForBulkLoad = false) throw(index_exception);

            ~CorpusIndex();

            const uint8_t GetPrefixLength() const {
                return prefixLength;
            }

            void PutBatch(UpdateBatch &batch) throw(index_exception);

            void ForceCompaction();

            const vector<seqid_t> &GetStreamsStatus() const {
                return streams;
            }

            int64_t GetStorageSize() {
                return storageSize;
            }

        private:
            const uint8_t prefixLength;

            rocksdb::DB *db;
            vector<seqid_t> streams;
            int64_t storageSize;
        };

    }
}


#endif //SAPT_CORPUSINDEX_H
