//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_UPDATEBATCH_H
#define SAPT_UPDATEBATCH_H

#include <mmt/IncrementalModel.h>
#include <boost/functional/hash.hpp>
#include <sapt/position.h>

using namespace std;

struct prefix_hash {
    size_t operator()(const vector<mmt::wid_t> &c) const {
        return boost::hash_range(c.begin(), c.end());
    }
};

namespace mmt {
    namespace sapt {

        class UpdateBatch {
            friend class CorpusIndex;

            friend class CorpusStorage;

        public:
            UpdateBatch(uint8_t prefixLength, size_t maxSize, const vector<seqid_t> &streams);

            bool Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                     const std::vector<wid_t> &target, const alignment_t &alignment);

            bool Add(const domain_t domain, const std::vector<wid_t> &source,
                     const std::vector<wid_t> &target, const alignment_t &alignment);

            inline size_t GetSize() const {
                return size;
            }

            inline size_t GetMaxSize() const {
                return maxSize;
            }

            void Reset(const vector<seqid_t> &streams);

            void Clear();

            const vector<seqid_t> &GetStreams() const {
                return streams;
            }

        private:
            typedef unordered_map<vector<wid_t>, vector<position_t>, prefix_hash> prefixmap_t;

            const uint8_t prefixLength;
            const size_t maxSize;
            size_t size;

            int64_t currentOffset;
            int64_t baseOffset;
            int64_t storageSize;
            vector<seqid_t> streams;
            vector<vector<char>> encodedData;
            unordered_map<domain_t, prefixmap_t> prefixes;

            bool SetStreamIfValid(stream_t stream, seqid_t sentence);

            inline void AddToBatch(const domain_t domain, const std::vector<wid_t> &source,
                                   const std::vector<wid_t> &target, const alignment_t &alignment);

        };

    }
}


#endif //SAPT_UPDATEBATCH_H
