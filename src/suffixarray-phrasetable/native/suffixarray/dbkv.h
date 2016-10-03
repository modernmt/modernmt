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
            kTargetPrefixKeyType = 2
        };

        /* Keys */

        static inline string MakeEmptyKey(char type) {
            char bytes[1];
            bytes[0] = type;
            string key(bytes, 1);
            return key;
        }

        static inline string
        MakePrefixKey(length_t prefixLength, bool isSource, domain_t domain,
                      const vector<wid_t> &phrase, size_t offset, size_t length) {
            size_t size = 1 + sizeof(domain_t) + prefixLength * sizeof(wid_t);
            char *bytes = new char[size];
            bytes[0] = isSource ? kSourcePrefixKeyType : kTargetPrefixKeyType;

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

        static inline domain_t GetDomainFromKey(const char *data, length_t prefixLength) {
            size_t i = 1 + prefixLength * sizeof(wid_t);
            return ReadUInt32(data, i);
        }

        /* Values */

        static inline string SerializeGlobalInfo(const vector<seqid_t> &streams, int64_t storageSize) {
            size_t size = 8 + streams.size() * sizeof(seqid_t);
            char *bytes = new char[size];
            size_t i = 0;

            WriteInt64(bytes, &i, storageSize);
            for (auto id = streams.begin(); id != streams.end(); ++id)
                WriteUInt64(bytes, &i, *id);

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

    }
}

#endif //SAPT_DBKV_H
