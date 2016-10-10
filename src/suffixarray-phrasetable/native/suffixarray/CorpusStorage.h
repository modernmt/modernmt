//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_CORPUSSTORAGE_H
#define SAPT_CORPUSSTORAGE_H

#include <cstddef>
#include <string>
#include <vector>
#include <mutex>
#include <mmt/sentence.h>

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
                          alignment_t *outAlignment) const;

            int64_t Append(const vector<wid_t> &sourceSentence, const vector<wid_t> &targetSentence,
                           const alignment_t &alignment) throw(storage_exception);

            int64_t Flush() throw(storage_exception);

        private:
            int fd;
            char *data;
            size_t dataLength;

            mutex writeMutex;

            ssize_t MemoryMap();
        };

    }
}


#endif //SAPT_CORPUSSTORAGE_H
