//
// Created by Davide  Caroselli on 15/02/17.
//

#include "StorageIterator.h"

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

#define IsValidOffset(offset) (0 <= offset && (size_t) offset < dataLength)

StorageIterator::StorageIterator(std::shared_ptr<StorageBucket> bucket, int64_t initialOffset)
        : bucket(bucket), dataLength(bucket->GetSize()), offset(initialOffset) {
}

bool StorageIterator::Next(std::vector<wid_t> *outSource, std::vector<wid_t> *outTarget, alignment_t *outAlignment,
                           int64_t *outNextOffset) {
    if (IsValidOffset(offset)) {
        outSource->clear();
        outTarget->clear();
        outAlignment->clear();

        offset = bucket->Retrieve(offset, outSource, outTarget, outAlignment);

        if (outNextOffset)
            *outNextOffset = offset < 0 ? StorageIterator::eof : offset;

        return true;
    } else {
        if (outNextOffset)
            *outNextOffset = StorageIterator::eof;

        return false;
    }
}