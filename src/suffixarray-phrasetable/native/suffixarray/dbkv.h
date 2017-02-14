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
            kGlobalInfoKeyType = 0,
            kSourcePrefixKeyType = 1,
            kTargetCountKeyType = 2,
            kDeletedDomainsType = 3
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

        /* Values */

        static inline string SerializeGlobalInfo(const vector<seqid_t> &streams, int64_t storageSize) {
            size_t size = 8 + streams.size() * sizeof(seqid_t);
            char *bytes = new char[size];
            size_t i = 0;

            WriteInt64(bytes, &i, storageSize);
            for (auto id = streams.begin(); id != streams.end(); ++id)
                WriteInt64(bytes, &i, *id);

            string result = string(bytes, size);
            delete[] bytes;

            return result;
        }

        static inline bool
        DeserializeGlobalInfo(const char *data, size_t bytes_size, int64_t *outStorageSize,
                              vector<seqid_t> *outStreams) {
            if (bytes_size < 8 || bytes_size % 8 != 0)
                return false;

            size_t ptr = 0;

            *outStorageSize = ReadInt64(data, &ptr);

            size_t size = (bytes_size - 8) / sizeof(seqid_t);
            outStreams->resize(size, 0);

            for (size_t i = 0; i < size; ++i)
                outStreams->at(i) = ReadUInt64(data, &ptr);

            return true;
        }

        static inline string SerializeCount(uint64_t count) {
            char bytes[8];
            WriteUInt64(bytes, (size_t) 0, count);

            return string(bytes, 8);
        }

        static inline uint64_t DeserializeCount(const char *data, size_t size) {
            if (size != 8)
                return 0;

            return ReadUInt64(data, (size_t) 0);
        }

        static inline string SerializeDeletedDomains(const std::unordered_set<domain_t> &domains) {
            size_t size = domains.size() * sizeof(domain_t);
            char *bytes = new char[size];
            size_t i = 0;

            for (auto domain = domains.begin(); domain != domains.end(); ++domain)
                WriteUInt64(bytes, &i, *domain);

            string result = string(bytes, size);
            delete[] bytes;

            return result;
        }

        static inline bool
        DeserializeDeletedDomains(const char *data, size_t bytes_size, unordered_set<domain_t> *outDomains) {
            if (bytes_size < sizeof(domain_t) || bytes_size % sizeof(domain_t) != 0)
                return false;

            size_t length = bytes_size / sizeof(domain_t);

            size_t ptr = 0;
            for (size_t i = 0; i < length; ++i)
                outDomains->insert(ReadUInt32(data, &ptr));

            return true;
        }
    }
}

#endif //SAPT_DBKV_H
