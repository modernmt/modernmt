//
// Created by Davide  Caroselli on 10/10/16.
//

#include <util/hashutils.h>
#include <iostream>
#include "Collector.h"

using namespace mmt;
using namespace mmt::sapt;

Collector::Collector(CorpusStorage *storage, rocksdb::DB *db, length_t prefixLength, const context_t *context,
                     bool searchInBackground) : prefixLength(prefixLength), storage(storage) {
    phrase.reserve(20); // typical max phrase length

    if (context && !context->empty()) {
        inDomainStates.resize(context->size());

        for (size_t i = 0; i < context->size(); ++i) {
            inDomainStates[i].cursor.reset(
                    PrefixCursor::NewDomainCursor(db, prefixLength, true, context->at(i).domain)
            );
        }
    }

    if (searchInBackground) {
        backgroundState = new state_t();
        backgroundState->cursor.reset(PrefixCursor::NewGlobalCursor(db, prefixLength, true, context));
    }
}

void Collector::Extend(const vector<wid_t> &words, size_t limit, vector<sample_t> &outSamples) {
    phrase.insert(phrase.end(), words.begin(), words.end());
    unsigned int shuffleSeed = max(1U, words_hash(phrase));

    // Get in-context samples
    vector<location_t> inContextSamples;
    size_t inContextSize = 0;

    for (auto state = inDomainStates.begin(); state != inDomainStates.end(); /* no increment */) {
        PrefixCursor *cursor = state->cursor.get();

        PostingList *postingList = NULL;
        size_t collected = CollectLocations(cursor, phrase, prefixLength, state->phraseOffset, &postingList);
        state->postingList.reset(postingList);

        state->phraseOffset = phrase.size();

        if (collected > 0) {
            bool breakLoop;

            inContextSize = min(limit, inContextSize + collected);

            if (limit == 0 || inContextSize < limit) {
                state->postingList->GetLocations(inContextSamples);
                breakLoop = false;
            } else {
                state->postingList->GetLocations(inContextSamples, limit - inContextSize, shuffleSeed);
                breakLoop = true;
            }

            if (phrase.size() < prefixLength) {
                // No need to cache Posting Lists shorter than prefixLength
                state->postingList.reset();
            }

            if (breakLoop)
                break;

            ++state;
        } else {
            state = inDomainStates.erase(state);
        }
    }

    // Get out-context samples
    vector<location_t> outContextSamples;

    if (backgroundState && (limit == 0 || inContextSize < limit)) {
        PrefixCursor *cursor = backgroundState->cursor.get();
        PostingList *postingList = NULL;
        size_t collected = CollectLocations(cursor, phrase, prefixLength, backgroundState->phraseOffset, &postingList);
        backgroundState->postingList.reset(postingList);
        backgroundState->phraseOffset = phrase.size();

        if (collected > 0) {
            backgroundState->postingList->GetLocations(outContextSamples, limit == 0 ? 0 : limit - inContextSize);

            if (phrase.size() < prefixLength) {
                // No need to cache Posting Lists shorter than prefixLength
                backgroundState->postingList.reset();
            }
        } else {
            delete backgroundState;
            backgroundState = NULL;
        }
    }

    outSamples.clear();

    if (inContextSamples.size() > 0)
        Retrieve(inContextSamples, outSamples);
    if (outContextSamples.size() > 0)
        Retrieve(outContextSamples, outSamples);
}

size_t Collector::CollectLocations(PrefixCursor *cursor, const vector<wid_t> &phrase, length_t prefixLength,
                                   size_t offset, PostingList **postingList) {
    if (offset == 0)
        assert(postingList == NULL || (*postingList) == NULL);

    // if postingList is NULL, create a local variable to be used by the method
    PostingList *localPostingList = NULL;
    if (postingList == NULL)
        postingList = &localPostingList;

    // adjust offset: if no previous posting list is provided, we need to iterate from
    //                the very beginning
    if (*postingList == NULL)
        offset = 0;

    // collect the locations
    size_t phraseLength = phrase.size();

    if (phraseLength < prefixLength) {
        CollectPhraseLocations(cursor, phrase, 0, phrase.size(), postingList);
    } else {
        size_t start = offset;

        while (start < phraseLength) {
            if (start + prefixLength > phraseLength)
                start = phraseLength - prefixLength;

            if (start == 0) {
                CollectPhraseLocations(cursor, phrase, start, prefixLength, postingList);
            } else {
                PostingList *successors = NULL;
                CollectPhraseLocations(cursor, phrase, start, prefixLength, &successors);

                (*postingList)->Retain(successors, start);
            }

            if ((*postingList)->empty())
                break;

            start += prefixLength;
        }
    }

    // save result and delete local posting list if needed
    size_t result = (*postingList)->size();
    if (localPostingList)
        delete localPostingList;
    return result;
}

void Collector::CollectPhraseLocations(PrefixCursor *cursor, const vector<wid_t> &phrase, size_t offset, size_t length,
                                       PostingList **postingList) {
    if (*postingList == NULL)
        *postingList = new PostingList();

    for (cursor->Seek(phrase, offset, length); cursor->HasNext(); cursor->Next()) {
        cursor->CollectValue(*postingList);
    }
}

void Collector::Retrieve(const vector<location_t> &locations, vector<sample_t> &outSamples) {
    outSamples.reserve(outSamples.size() + locations.size());

    sample_t *lastSample = NULL;
    int64_t lastPointer = -1;

    for (auto location = locations.begin(); location != locations.end(); ++location) {
        if (lastSample && lastPointer == location->pointer) {
            lastSample->offsets.push_back(location->offset);
        } else {
            sample_t sample;
            sample.domain = location->domain;
            sample.offsets.push_back(location->offset);
            storage->Retrieve(location->pointer, &sample.source, &sample.target, &sample.alignment);

            outSamples.push_back(sample);

            lastPointer = location->pointer;
            lastSample = &(outSamples[outSamples.size() - 1]);
        }
    }
}