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

static inline void WriteSentence(int fd, const vector<mmt::wid_t> &sentence) throw(storage_exception) {
    size_t size = (sentence.size() + 1) * sizeof(mmt::wid_t);
    char *buffer = new char[size];
    int64_t ptr = 0;

    for (auto word = sentence.begin(); word != sentence.end(); ++word)
        WriteInt32(buffer, &ptr, *word);
    WriteInt32(buffer, &ptr, kEndOfSentenceSymbol);

    ssize_t result = write(fd, buffer, size);
    delete[] buffer;

    if (result != size)
        throw storage_exception("Unable to write to corpus storage");
}

static inline void WriteAlignment(int fd, const mmt::alignment_t &alignment) throw(storage_exception) {
    size_t size = 4 + alignment.size() * 2 * sizeof(mmt::length_t);
    char *buffer = new char[size];
    int64_t ptr = 0;

    WriteInt32(buffer, &ptr, (uint32_t) alignment.size());
    for (auto a = alignment.begin(); a != alignment.end(); ++a) {
        WriteInt16(buffer, &ptr, a->first);
        WriteInt16(buffer, &ptr, a->second);
    }

    ssize_t result = write(fd, buffer, size);
    delete[] buffer;

    if (result != size)
        throw storage_exception("Unable to write to corpus storage");
}

static inline bool ReadSentence(const char *data, size_t data_length, int64_t *ptr, vector<mmt::wid_t> *outSentence) {
    bool endSymbolFound = false;

    while (!endSymbolFound && (*ptr + 4 <= data_length)) {
        mmt::wid_t word = ReadInt32(data, ptr);

        if (word == kEndOfSentenceSymbol)
            endSymbolFound = true;
        else
            outSentence->push_back(word);
    }

    return endSymbolFound;
}

static inline bool ReadAlignment(const char *data, size_t data_length, int64_t *ptr, mmt::alignment_t *outAlignment) {
    if (*ptr + 4 > data_length)
        return false;

    uint32_t length = ReadInt32(data, ptr);
    outAlignment->reserve(length);

    if (*ptr + (4 * length) > data_length)
        return false;

    for (uint32_t i = 0; i < length; i++) {
        mmt::length_t a = ReadInt16(data, ptr);
        mmt::length_t b = ReadInt16(data, ptr);

        outAlignment->push_back(make_pair(a, b));
    }

    return true;
}

/* CorpusStorage */

CorpusStorage::CorpusStorage(const string &filepath, int64_t size) throw(storage_exception) : data(NULL),
                                                                                              data_length(0) {
    mode_t mode = S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH;
    fd = open(filepath.c_str(), O_RDWR | O_CREAT, mode);

    if (fd == -1)
        throw storage_exception("Cannot open file " + filepath);

    if (size > 0) {
        if (lseek(fd, size, SEEK_SET) != size)
            throw storage_exception("Invalid file size specified: " + to_string(size));
    } else {
        lseek(fd, 0, SEEK_END);
    }

    MMap();
}

CorpusStorage::~CorpusStorage() {
    fsync(fd);
    close(fd);
}

bool
CorpusStorage::Retrieve(int64_t offset, vector<wid_t> *outSourceSentence, vector<wid_t> *outTargetSentence,
                        mmt::alignment_t *outAlignment) {
    if (offset >= data_length)
        return false;

    int64_t ptr = offset;

    if (!ReadSentence(data, data_length, &ptr, outSourceSentence)) return false;
    if (!ReadSentence(data, data_length, &ptr, outTargetSentence)) return false;

    return ReadAlignment(data, data_length, &ptr, outAlignment);
}

int64_t CorpusStorage::Append(const vector<wid_t> &sourceSentence, const vector<wid_t> &targetSentence,
                              const mmt::alignment_t &alignment) throw(storage_exception) {
    int64_t position = (int64_t) lseek(fd, 0, SEEK_CUR);

    WriteSentence(fd, sourceSentence);
    WriteSentence(fd, targetSentence);
    WriteAlignment(fd, alignment);

    return position;
}

int64_t CorpusStorage::Flush() throw(storage_exception) {
    if (fsync(fd) == -1)
        throw storage_exception("Failed to flush data to disk");

    MMap();

    return (int64_t) data_length;
}

void CorpusStorage::MMap() throw(storage_exception) {
    size_t size = (size_t) lseek(fd, 0, SEEK_CUR);

    if (size == 0)
        return;

    if (data == NULL) {
        data = (char *) mmap(NULL, size, PROT_READ, MAP_SHARED, fd, 0);

        if (data == MAP_FAILED)
            throw storage_exception("Unable to mmap corpus storage");
    } else {
#ifdef __APPLE__
        if (munmap(data, data_length) == -1)
            throw storage_exception("Unable to unmap corpus storage");

        data = (char *) mmap(NULL, size, PROT_READ, MAP_SHARED, fd, 0);
#else
        data = (char *) mremap(data, data_length, size, MREMAP_MAYMOVE);
#endif
        if (data == MAP_FAILED)
            throw storage_exception("Unable to mmap corpus storage");
    }


    data_length = size;
}


