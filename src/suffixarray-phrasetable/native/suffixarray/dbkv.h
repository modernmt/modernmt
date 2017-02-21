//
// Created by Davide  Caroselli on 30/09/16.
//

#ifndef SAPT_DBKV_H
#define SAPT_DBKV_H

#include <string>
#include <util/ioutils.h>
#include <mmt/sentence.h>
#include "SuffixArray.h"

static_assert(sizeof(mmt::wid_t) == 4, "Current implementation works only with 32-bit word ids");
static_assert(sizeof(mmt::domain_t) == 4, "Current implementation works only with 32-bit domain id");
static_assert(sizeof(mmt::length_t) == 2, "Current implementation works only with 16-bit sentence length");

using namespace std;

namespace mmt {
    namespace sapt {

        enum KeyType {
            kStreamsKeyType = 0,
            kStorageManifestKeyType = 1,
            kDeletedDomainKeyType = 2,
            kPendingDeletionKeyType = 3,

            kSourcePrefixKeyType = 4,
            kTargetCountKeyType = 5,
        };

        /* Keys */

        static inline string MakeEmptyKey(char type) {
            char bytes[1];
            bytes[0] = type;
            string key(bytes, 1);
            return key;
        }

        static inline string
        MakePrefixKey(length_t prefixLength, domain_t domain,
                      const vector<wid_t> &phrase, size_t offset, size_t length) {
            size_t size = 1 + sizeof(domain_t) + prefixLength * sizeof(wid_t);
            char *bytes = new char[size];
            bytes[0] = kSourcePrefixKeyType;

            size_t ptr = 1;

            for (size_t i = 0; i < length; ++i)
                WriteUInt32(bytes, &ptr, phrase[offset + i]);
            for (size_t i = length; i < prefixLength; ++i)
                WriteUInt32(bytes, &ptr, 0);

            WriteUInt32(bytes, &ptr, domain);

            string key(bytes, size);
            delete[] bytes;

            return key;
        }

        static inline string
        MakeCountKey(length_t prefixLength, const vector<wid_t> &phrase, size_t offset, size_t length) {
            size_t size = 1 + sizeof(domain_t) + prefixLength * sizeof(wid_t);
            char *bytes = new char[size];
            bytes[0] = kTargetCountKeyType;

            size_t ptr = 1;

            for (size_t i = 0; i < length; ++i)
                WriteUInt32(bytes, &ptr, phrase[offset + i]);
            for (size_t i = length; i < prefixLength; ++i)
                WriteUInt32(bytes, &ptr, 0);

            WriteUInt32(bytes, &ptr, 0); // no domain info

            string key(bytes, size);
            delete[] bytes;

            return key;
        }

        static inline string MakeDomainDeletionKey(domain_t domain) {
            char bytes[5];
            bytes[0] = kDeletedDomainKeyType;

            size_t ptr = 1;
            WriteUInt32(bytes, &ptr, domain);

            return string(bytes, 5);
        }

        static inline KeyType GetKeyTypeFromKey(const char *data, length_t prefixLength) {
            return (KeyType) data[0];
        }

        static inline domain_t GetDomainFromKey(const char *data, length_t prefixLength) {
            size_t i = 1 + prefixLength * sizeof(wid_t);
            return ReadUInt32(data, i);
        }

        static inline void GetWordsFromKey(const char *data, length_t prefixLength, vector<wid_t> &words) {
            size_t offset = 1;
            for (size_t i = 0; i < prefixLength; ++i) {
                words.push_back(ReadUInt32(data, offset));
                offset += sizeof(wid_t);
            }
        }

        static inline domain_t GetDomainFromDeletionKey(const char *data) {
            return ReadUInt32(data, 1);
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

        static inline string SerializeCount(int64_t count) {
            char bytes[8];
            WriteInt64(bytes, (size_t) 0, count);

            return string(bytes, 8);
        }

        static inline int64_t DeserializeCount(const char *data, size_t size) {
            if (size != 8)
                return 0;

            return ReadInt64(data, (size_t) 0);
        }

        static inline string SerializePendingDeletionData(domain_t domain, int64_t offset) {
            char bytes[12];
            size_t ptr = 0;

            WriteUInt32(bytes, &ptr, domain);
            WriteInt64(bytes, &ptr, offset);

            string value(bytes, 12);
            return value;
        }

        static inline bool
        DeserializePendingDeletionData(const char *data, size_t bytes_size, domain_t *outDomain, int64_t *outOffset) {
            if (bytes_size != 12)
                return false;

            size_t ptr = 0;
            *outDomain = ReadUInt32(data, &ptr);
            *outOffset = ReadInt64(data, &ptr);

            return true;
        }
    }
}

#endif //SAPT_DBKV_H
