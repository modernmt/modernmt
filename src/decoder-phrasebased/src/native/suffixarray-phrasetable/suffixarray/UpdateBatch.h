//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_UPDATEBATCH_H
#define SAPT_UPDATEBATCH_H

#include <mmt/IncrementalModel.h>
#include <boost/functional/hash.hpp>

using namespace std;

namespace mmt {
    namespace sapt {

        class UpdateBatch {
            friend class SuffixArray;

        public:
            UpdateBatch(size_t maxSize, const vector<seqid_t> &streams);

            bool Add(const updateid_t &id, const memory_t memory, const std::vector<wid_t> &source,
                     const std::vector<wid_t> &target, const alignment_t &alignment);

            bool Add(const memory_t memory, const std::vector<wid_t> &source,
                     const std::vector<wid_t> &target, const alignment_t &alignment);

            bool Delete(const updateid_t &id, const memory_t memory);

            bool IsEmpty();

            void Reset(const vector<seqid_t> &streams);

            void Clear();

            const vector<seqid_t> &GetStreams() const {
                return streams;
            }

        private:

            struct sentencepair_t {
                memory_t memory;
                vector<wid_t> source;
                vector<wid_t> target;
                alignment_t alignment;

                sentencepair_t() {};
            };

            const size_t maxSize;

            vector<seqid_t> streams;
            vector<sentencepair_t> data;
            vector<memory_t> deletions;

            bool SetStreamIfValid(stream_t stream, seqid_t sentence);

        };

    }
}


#endif //SAPT_UPDATEBATCH_H
