//
// Created by Davide  Caroselli on 15/02/17.
//

#include "StorageIterator.h"

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

#define IsValidOffset(offset) (0 <= offset && (size_t) offset < dataLength)

StorageIterator::StorageIterator(std::shared_ptr<StorageBucket> bucket, size_t initialOffset)
        : bucket(bucket), dataLength(bucket->GetSize()), offset((int64_t) initialOffset) {
}

size_t StorageIterator::Next(vector<wid_t> *outSource, vector<wid_t> *outTarget,
                             alignment_t *outAlignment) throw(storage_exception) {
    if (IsValidOffset(offset)) {
        outSource->clear();
        outTarget->clear();
        outAlignment->clear();

        offset = bucket->Retrieve(offset, outSource, outTarget, outAlignment);

        return (size_t) offset;
    } else {
        return StorageIterator::eof;
    }
}
