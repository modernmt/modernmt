//
// Created by Davide  Caroselli on 15/02/17.
//

#ifndef SAPT_STORAGEBUCKET_H
#define SAPT_STORAGEBUCKET_H

#include <string>
#include <mutex>
#include <mmt/sentence.h>
#include <util/ioutils.h>
#include "storage_exception.h"

namespace mmt {
    namespace sapt {

        class StorageBucket {
            friend class CorporaStorage;

            friend class StorageIterator;

        public:
            ~StorageBucket();

        private:
            const std::string filepath;

            int fd;
            char *data;
            size_t dataLength;
            bool deleteOnClose;

            std::mutex writeMutex;

            StorageBucket(const std::string &filepath, int64_t size = -1) throw(storage_exception);

            int64_t Retrieve(int64_t offset, std::vector<wid_t> *outSourceSentence,
                             std::vector<wid_t> *outTargetSentence, alignment_t *outAlignment) const;

            int64_t Append(const std::vector<wid_t> &sourceSentence, const std::vector<wid_t> &targetSentence,
                           const alignment_t &alignment) throw(storage_exception);

            int64_t Flush() throw(storage_exception);

            void MarkForDeletion();

            const size_t GetSize() const {
                return dataLength;
            }

            ssize_t MemoryMap();

        };

    }
}


#endif //SAPT_STORAGEBUCKET_H
