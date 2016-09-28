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

            bool Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                     const std::vector<wid_t> &target, const alignment_t &alignment);

            bool Add(const domain_t domain, const std::vector<wid_t> &source,
                     const std::vector<wid_t> &target, const alignment_t &alignment);

            inline size_t GetSize() const {
                return data.size();
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

            struct sentencepair_t {
                domain_t domain;
                vector<wid_t> source;
                vector<wid_t> target;
                alignment_t alignment;

                sentencepair_t() {};
            };

            const size_t maxSize;

            vector<seqid_t> streams;
            vector<sentencepair_t> data;

            bool SetStreamIfValid(stream_t stream, seqid_t sentence);

        };

    }
}


#endif //SAPT_UPDATEBATCH_H
