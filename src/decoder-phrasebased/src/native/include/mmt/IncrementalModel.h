//
// Created by Davide  Caroselli on 02/09/16.
//

#ifndef MMT_COMMON_INTERFACES_INCREMENTALMODEL_H
#define MMT_COMMON_INTERFACES_INCREMENTALMODEL_H

#include <mmt/sentence.h>

namespace mmt {

    typedef int8_t channel_t;
    typedef int64_t seqid_t;

    struct translation_unit {
        channel_t channel;
        seqid_t position;

        memory_t memory;
        std::vector<wid_t> source;
        std::vector<wid_t> target;
        alignment_t alignment;

        translation_unit() : channel(0), position(0), memory(0), source(), target(), alignment() {};
    };

    struct deletion {
        channel_t channel;
        seqid_t position;
        memory_t memory;

        deletion() : channel(0), position(0), memory(0) {};
    };

    struct update_batch_t {
        std::vector<translation_unit> translation_units;
        std::vector<deletion> deletions;
        std::unordered_map<channel_t, seqid_t> channelPositions;

        update_batch_t() : translation_units(), deletions(), channelPositions() {};
    };

    class IncrementalModel {
    public:

        virtual void OnUpdateBatchReceived(const update_batch_t &batch) = 0;

        /**
         * Retrieve the latest update ids registered in the system. These values must be updated
         * at every OnUpdateBatchReceived() request, however this method will be usually called once at the system
         * startup in order to filter updates with ids less than the ones returned by this method
         *
         * @return an array of all the latest registered update ids, one for every stream.
         */
        virtual std::unordered_map<channel_t, seqid_t> GetLatestUpdatesIdentifier() = 0;

    };
}


#endif //MMT_COMMON_INTERFACES_INCREMENTALMODEL_H