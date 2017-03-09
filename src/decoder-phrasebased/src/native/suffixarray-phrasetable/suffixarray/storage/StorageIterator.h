//
// Created by Davide  Caroselli on 15/02/17.
//

#ifndef SAPT_STORAGEITERATOR_H
#define SAPT_STORAGEITERATOR_H

#include <cstddef>
#include <memory>
#include <mmt/sentence.h>
#include "storage_exception.h"
#include "StorageBucket.h"

namespace mmt {
    namespace sapt {

        class StorageIterator {
            friend class CorporaStorage;

        public:
            static const int64_t eof = -1;

            bool Next(std::vector<wid_t> *outSource, std::vector<wid_t> *outTarget,
                      alignment_t *outAlignment, int64_t *outNextOffset);

        private:
            std::shared_ptr<StorageBucket> bucket;
            size_t dataLength;
            int64_t offset;

            StorageIterator(std::shared_ptr<StorageBucket> bucket, int64_t initialOffset = 0);

        };

    }
}

#endif //SAPT_STORAGEITERATOR_H
