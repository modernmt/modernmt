//
// Created by Davide  Caroselli on 02/09/16.
//

#ifndef MMT_COMMON_INTERFACES_INCREMENTALMODEL_H
#define MMT_COMMON_INTERFACES_INCREMENTALMODEL_H

#include <mmt/sentence.h>

namespace mmt {

    typedef int8_t stream_t;
    typedef int64_t seqid_t;

    struct updateid_t {
        stream_t stream_id;
        seqid_t sentence_id;

        updateid_t(stream_t stream_id = 0, seqid_t sentence_id = 0) : stream_id(stream_id), sentence_id(sentence_id) {};

    };

    class IncrementalModel {
    public:

        /**
         * Add a new aligned sentence pair to this model.
         *
         * The invocation of this method is always made by exactly one thread at time, so
         * the implementation of this method does not need to handle exclusive access to resources
         * or, more in general, worry about multi-thread execution.
         *
         * It is not required to be immediately persistent: the sentence pairs can be added to
         * a batch that, at some point in the future, will be flushed into the model. However it is
         * highly recommended that:
         *      - The execution of this method never take more than 1 second.
         *      - A pending update does not wait more than 2 seconds before being written to the model.
         *
         * An update always comes with an identifier consisting in a stream-id, the source identifier, and
         * a sequential id that identifies the sentence pair within the sequence. Within a particular stream,
         * a newer update has always a higher sequential id of an older one. However it is possible to receive
         * and update with an id that is lower than the current newest value for a particular stream; in
         * this case the model must recognize this situation and discard the update.
         *
         * On the contrary, there are no guarantees for sequential ids with different stream-id:
         * a sequential id is valid only within its stream.
         * Thus an IncrementalModel must keep track of the higher id value for each stream.
         *
         * It is mandatory that the latest sequential id is stored atomically with the latest flush.
         * If the actual implementation writes the data and updates the latest id in two separate steps,
         * there is the chance to leave the model in a inconsistent state and to receive the same
         * update twice.
         *
         * @param id the update unique id.
         * @param domain the domain id.
         * @param source the source sentence.
         * @param target the target sentence.
         * @param alignment the sentence pair alignment.
         */
        virtual void
        Add(const updateid_t &id, const domain_t domain,
            const std::vector<wid_t> &source, const std::vector <wid_t> &target,
            const alignment_t &alignment) = 0;

        /**
         * Requests the deletion of the specified domain.
         *
         * The invocation of this method is always made by exactly one thread at time, so
         * the implementation of this method does not need to handle exclusive access to resources
         * or, more in general, worry about multi-thread execution.
         *
         * @param id the update unique id.
         * @param domain the domain id.
         */
        virtual void Delete(const updateid_t &id, const domain_t domain) = 0;

        /**
         * Retrieve the latest update ids registered in the system. These values must be updated
         * at every Add() request, however this method will be usually called once at the system
         * startup in order to filter updates with ids less than the ones returned by this method
         *
         * @return an array of all the latest registered update ids, one for every stream.
         */
        virtual std::unordered_map<stream_t, seqid_t> GetLatestUpdatesIdentifier() = 0;

    };
}


#endif //MMT_COMMON_INTERFACES_INCREMENTALMODEL_H
