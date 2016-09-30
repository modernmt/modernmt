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
        MakePrefixKey(bool isSource, domain_t domain, const vector<wid_t> &phrase, size_t offset, size_t length) {
            size_t size = 1 + sizeof(domain_t) + length * sizeof(wid_t);
            char *bytes = new char[size];
            bytes[0] = isSource ? kSourcePrefixKeyType : kTargetPrefixKeyType;

            size_t ptr = 1;

            WriteUInt32(bytes, &ptr, domain);
            for (size_t i = 0; i < length; ++i)
                WriteUInt32(bytes, &ptr, phrase[offset + i]);

            string key(bytes, size);
            delete[] bytes;

            return key;
        }

        /* Values */

//        static inline string SerializePositionsList(const vector<location_t> &positions) {
//            size_t size = positions.size() * (sizeof(int64_t) + sizeof(length_t));
//            char *bytes = new char[size];
//
//            size_t i = 0;
//            for (auto position = positions.begin(); position != positions.end(); ++position) {
//                WriteInt64(bytes, &i, position->offset);
//                WriteUInt16(bytes, &i, position->sentence_offset);
//            }
//
//            string value(bytes, size);
//            delete[] bytes;
//
//            return value;
//        }
//
//        static inline void
//        DeserializePositionsList(domain_t domain, const char *data, size_t bytes_size, positionsmap_t &outPositions,
//                                 positionsmap_t *coveredPositions = NULL) {
//            size_t entry_size = sizeof(int64_t) + sizeof(length_t);
//
//            if (bytes_size % entry_size != 0)
//                return;
//
//            size_t count = bytes_size / entry_size;
//            outPositions.reserve(outPositions.size() + count);
//
//            size_t ptr = 0;
//            for (size_t i = 0; i < count; ++i) {
//                int64_t offset = ReadInt64(data, &ptr);
//                length_t sentence_offset = ReadUInt16(data, &ptr);
//
//                if (coveredPositions && coveredPositions->find(offset) != coveredPositions->end())
//                    continue;
//
//                auto &value = outPositions[offset];
//                value.first = domain;
//                value.second.push_back(sentence_offset);
//            }
//        }

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
