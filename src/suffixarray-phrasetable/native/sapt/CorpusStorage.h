//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_CORPUSSTORAGE_H
#define SAPT_CORPUSSTORAGE_H

#include <cstddef>
#include <string>
#include <vector>
#include <mmt/sentence.h>
#include "UpdateBatch.h"

using namespace std;

namespace mmt {
    namespace sapt {

        class storage_exception : public exception {
        public:
            storage_exception(const string &msg) : message(msg) {};

            virtual const char *what() const throw() override {
                return message.c_str();
            }

        private:
            string message;
        };

        class CorpusStorage {
        public:
            CorpusStorage(const string &filepath, int64_t size = -1) throw(storage_exception);

            ~CorpusStorage();

            bool Retrieve(int64_t offset, vector<wid_t> *outSourceSentence, vector<wid_t> *outTargetSentence,
                          alignment_t *outAlignment);

            static void Encode(const vector<wid_t> &sourceSentence, const vector<wid_t> &targetSentence,
                               const alignment_t &alignment, vector<char> *output);

            void PutBatch(UpdateBatch &batch) throw(storage_exception);

            int64_t Flush() throw(storage_exception);

        private:
            int fd;
            char *data;
            size_t data_length;

            void MMap() throw(storage_exception);
        };

    }
}


#endif //SAPT_CORPUSSTORAGE_H
