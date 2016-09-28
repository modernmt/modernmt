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
                                                                                              data_length(0) {
    mode_t mode = S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH;
    fd = open(filepath.c_str(), O_RDWR | O_CREAT, mode);

    if (fd == -1)
        throw storage_exception("Cannot open file " + filepath);

    if (size < 0) {
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

    size_t ptr = offset;

    if (!ReadSentence(data, data_length, &ptr, outSourceSentence)) return false;
    if (!ReadSentence(data, data_length, &ptr, outTargetSentence)) return false;

    return ReadAlignment(data, data_length, &ptr, outAlignment);
}

void CorpusStorage::Encode(const vector<mmt::wid_t> &sourceSentence, const vector<mmt::wid_t> &targetSentence,
                           const mmt::alignment_t &alignment, vector<char> *output) {
    size_t size = SentenceLengthInBytes(sourceSentence) + SentenceLengthInBytes(targetSentence) +
                  AlignmentLengthInBytes(alignment);

    output->resize(size);
    char *data = output->data();
    size_t ptr = 0;

    WriteSentence(data, &ptr, sourceSentence);
    WriteSentence(data, &ptr, targetSentence);
    WriteAlignment(data, &ptr, alignment);
}

void CorpusStorage::PutBatch(UpdateBatch &batch) throw(storage_exception) {
    batch.baseOffset = (int64_t) lseek(fd, 0, SEEK_CUR);

    for (auto data = batch.encodedData.begin(); data != batch.encodedData.end(); ++data) {
        char *buffer = data->data();
        size_t size = data->size();

        if (write(fd, buffer, size) != size)
            throw storage_exception("unable to append data to corpus storage");
    }

    batch.storageSize = Flush();
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


