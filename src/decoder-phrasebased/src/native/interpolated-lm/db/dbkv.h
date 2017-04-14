//
// Created by Davide  Caroselli on 30/09/16.
//

#ifndef SAPT_DBKV_H
#define SAPT_DBKV_H

#include <string>
#include "counts.h"
#include "ngram_hash.h"
#include <util/ioutils.h>
#include <mmt/sentence.h>
#include <mmt/IncrementalModel.h>

static_assert(sizeof(mmt::wid_t) == 4, "Current implementation works only with 32-bit word ids");
static_assert(sizeof(mmt::domain_t) == 4, "Current implementation works only with 32-bit domain id");
static_assert(sizeof(mmt::seqid_t) == 8, "Current version only supports 64-bit seqid_t");
static_assert(sizeof(mmt::ilm::count_t) == 4, "Current implementation works only with 32-bit counts");

using namespace std;

namespace mmt {
    namespace ilm {

        /* Keys */

        static inline string MakeNGramKey(domain_t domain, ngram_hash_t key) {
            char bytes[12];

            size_t ptr = 0;
            WriteUInt32(bytes, &ptr, domain);
            WriteUInt64(bytes, &ptr, key);

            return string(bytes, 12);
        }

        static inline string MakeDomainDeletionKey(domain_t domain) {
            char bytes[12];

            size_t ptr = 0;
            WriteUInt32(bytes, &ptr, 0);
            WriteUInt32(bytes, &ptr, 0);
            WriteUInt32(bytes, &ptr, domain);

            return string(bytes, 12);
        }

        static inline bool HasDomainDeletionPrefix(const char *data, size_t bytes_size) {
            if (bytes_size != 12)
                return false;

            return ReadUInt64(data, (size_t) 0) == 0;
        }

        static inline domain_t GetDomainFromDeletionKey(const char *data, size_t bytes_size) {
            if (bytes_size != 12)
                return 0;

            return ReadUInt32(data, 8);
        }

        static inline bool GetNGramKeyData(const char *data, size_t size, domain_t *outDomain, ngram_hash_t *outHash) {
            if (size != 12)
                return false;

            size_t ptr = 0;
            *outDomain = ReadUInt32(data, &ptr);
            *outHash = ReadUInt64(data, &ptr);

            return true;
        }

        /* Values */

        static inline string SerializeStreams(const vector<seqid_t> &streams) {
            size_t size = streams.size() * sizeof(seqid_t);
            char *bytes = new char[size];
            size_t i = 0;

            for (auto id = streams.begin(); id != streams.end(); ++id)
                WriteInt64(bytes, &i, *id);

            string result = string(bytes, size);
            delete[] bytes;

            return result;
        }

        static inline bool DeserializeStreams(const char *data, size_t bytes_size, vector<seqid_t> *outStreams) {
            if (bytes_size % 8 != 0)
                return false;

            size_t ptr = 0;

            size_t size = bytes_size / sizeof(seqid_t);
            outStreams->resize(size, 0);

            for (size_t i = 0; i < size; ++i)
                outStreams->at(i) = ReadUInt64(data, &ptr);

            return true;
        }

        static inline string SerializeCounts(counts_t counts) {
            char bytes[8];
            size_t ptr = 0;

            size_t length = VBELengthOfUInt32(counts.count) + VBELengthOfUInt32(counts.successors);

            if (length < 8) {
                VBEWriteUInt32(bytes, &ptr, counts.count);
                VBEWriteUInt32(bytes, &ptr, counts.successors);
            } else {
                WriteUInt32(bytes, &ptr, counts.count);
                WriteUInt32(bytes, &ptr, counts.successors);
                length = 8;
            }

            return string(bytes, length);
        }

        static inline bool DeserializeCounts(const char *data, size_t size, counts_t *output) {
            if (size > 8)
                return false;

            size_t ptr = 0;

            if (size == 8) {
                output->count = ReadUInt32(data, &ptr);
                output->successors = ReadUInt32(data, &ptr);
            } else {
                output->count = VBEReadUInt32(data, &ptr);
                output->successors = VBEReadUInt32(data, &ptr);
            }

            return true;
        }

    }
}

#endif //SAPT_DBKV_H
