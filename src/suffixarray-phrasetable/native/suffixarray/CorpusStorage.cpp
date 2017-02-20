//
// Created by Davide  Caroselli on 27/09/16.
//

#include <sys/fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <iostream>
#include <util/ioutils.h>
#include "CorpusStorage.h"

using namespace mmt::sapt;

static_assert(sizeof(mmt::wid_t) == 4, "Current implementation works only with 32-bit word ids");
static_assert(sizeof(mmt::length_t) == 2, "Current implementation works only with 16-bit sentence length");

static const mmt::wid_t kEndOfSentenceSymbol = 0;

#define SentenceLengthInBytes(sentence) ((sentence.size() + 1) * sizeof(mmt::wid_t))
#define AlignmentLengthInBytes(alignment) (4 + alignment.size() * 2 * sizeof(mmt::length_t))

static inline void WriteSentence(char *data, size_t *ptr, const vector<mmt::wid_t> &sentence) {
    for (auto word = sentence.begin(); word != sentence.end(); ++word)
        WriteUInt32(data, ptr, *word);
    WriteUInt32(data, ptr, kEndOfSentenceSymbol);
}

static inline void WriteAlignment(char *data, size_t *ptr, const mmt::alignment_t &alignment) throw(storage_exception) {
    WriteUInt32(data, ptr, (uint32_t) alignment.size());
    for (auto a = alignment.begin(); a != alignment.end(); ++a) {
        WriteUInt16(data, ptr, a->first);
        WriteUInt16(data, ptr, a->second);
    }
}

static inline bool ReadSentence(const char *data, size_t data_length, size_t *ptr, vector<mmt::wid_t> *outSentence) {
    bool endSymbolFound = false;

    while (!endSymbolFound && (*ptr + 4 <= data_length)) {
        mmt::wid_t word = ReadUInt32(data, ptr);

        if (word == kEndOfSentenceSymbol)
            endSymbolFound = true;
        else
            outSentence->push_back(word);
    }

    return endSymbolFound;
}

static inline bool ReadAlignment(const char *data, size_t data_length, size_t *ptr, mmt::alignment_t *outAlignment) {
    if (*ptr + 4 > data_length)
        return false;

    uint32_t length = ReadUInt32(data, ptr);
    outAlignment->reserve(length);

    if (*ptr + (4 * length) > data_length)
        return false;

    for (uint32_t i = 0; i < length; i++) {
        mmt::length_t a = ReadUInt16(data, ptr);
        mmt::length_t b = ReadUInt16(data, ptr);

        outAlignment->push_back(make_pair(a, b));
    }

    return true;
}

/* CorpusStorage */

CorpusStorage::CorpusStorage(const string &filepath, int64_t size) throw(storage_exception) : data(NULL),
                                                                                              dataLength(0) {
    mode_t mode = S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH;
    fd = open(filepath.c_str(), O_RDWR | O_CREAT, mode);

    if (fd == -1)
        throw storage_exception("Cannot open file " + filepath);

    if (size < 0) {
        lseek(fd, 0, SEEK_END);
    } else {
        if (lseek(fd, size, SEEK_SET) != size)
            throw storage_exception("Invalid file size specified: " + to_string(size));
    }

    ssize_t mappedSize = MemoryMap();
    dataLength = mappedSize > 0 ? (size_t) mappedSize : 0;
}

CorpusStorage::~CorpusStorage() {
    munmap(data, dataLength);

    fsync(fd);
    close(fd);
}

bool
CorpusStorage::Retrieve(int64_t offset, vector<wid_t> *outSourceSentence, vector<wid_t> *outTargetSentence,
                        mmt::alignment_t *outAlignment) const {
    size_t ptr = (size_t) offset;

    if (ptr >= dataLength)
        return false;

    if (!ReadSentence(data, dataLength, &ptr, outSourceSentence)) return false;
    if (!ReadSentence(data, dataLength, &ptr, outTargetSentence)) return false;

    return ReadAlignment(data, dataLength, &ptr, outAlignment);
}

int64_t CorpusStorage::Append(const vector<mmt::wid_t> &sourceSentence, const vector<mmt::wid_t> &targetSentence,
                              const mmt::alignment_t &alignment) throw(storage_exception) {
    size_t size = SentenceLengthInBytes(sourceSentence) + SentenceLengthInBytes(targetSentence) +
                  AlignmentLengthInBytes(alignment);

    char *buffer = new char[size];
    size_t i = 0;

    WriteSentence(buffer, &i, sourceSentence);
    WriteSentence(buffer, &i, targetSentence);
    WriteAlignment(buffer, &i, alignment);

    writeMutex.lock();
    int64_t ptr = (int64_t) lseek(fd, 0, SEEK_CUR);
    ssize_t writeResult = write(fd, buffer, size);
    writeMutex.unlock();

    if (writeResult != (ssize_t) size)
        throw storage_exception("unable to append data to corpus storage");

    return ptr;
}

int64_t CorpusStorage::Flush() throw(storage_exception) {
    int fsyncResult;
    ssize_t mmapSize = -1;

    writeMutex.lock();
    fsyncResult = fsync(fd);
    if (fsyncResult != -1) {
        mmapSize = MemoryMap();

        if (mmapSize > 0)
            dataLength = (size_t) mmapSize;
    }
    writeMutex.unlock();

    if (fsyncResult == -1 || mmapSize == -1)
        throw storage_exception("Failed to flush data to disk");

    return (int64_t) mmapSize;
}

ssize_t CorpusStorage::MemoryMap() {
    size_t size = (size_t) lseek(fd, 0, SEEK_CUR);

    if (size == 0)
        return 0;

    if (data == NULL) {
        data = (char *) mmap(NULL, size, PROT_READ, MAP_SHARED, fd, 0);

        if (data == MAP_FAILED)
            return -1;
    } else {
#ifdef __APPLE__
        if (munmap(data, dataLength) == -1)
            return -1;

        data = (char *) mmap(NULL, size, PROT_READ, MAP_SHARED, fd, 0);
#else
        data = (char *) mremap(data, dataLength, size, MREMAP_MAYMOVE);
#endif
        if (data == MAP_FAILED)
            return -1;
    }

    return size;
}

StorageIterator *CorpusStorage::NewIterator() const {
    return new StorageIterator(data, dataLength);
}

StorageIterator::StorageIterator(char *data, size_t dataLength) : data(data), dataLength(dataLength), offset(0) {
}

bool StorageIterator::Next(vector<wid_t> *outSource, vector<wid_t> *outTarget, alignment_t *outAlignment) {
    if (offset < dataLength) {
        outSource->clear();
        outTarget->clear();
        outAlignment->clear();

        if (!ReadSentence(data, dataLength, &offset, outSource))
            throw storage_exception("Broken corpus file at index " + offset);

        if (!ReadSentence(data, dataLength, &offset, outTarget))
            throw storage_exception("Broken corpus file at index " + offset);

        if (!ReadAlignment(data, dataLength, &offset, outAlignment))
            throw storage_exception("Broken corpus file at index " + offset);

        return true;
    } else {
        return false;
    }
}
