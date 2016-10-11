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
        size_t collected = CollectLocations(state->cursor.get(), phrase, prefixLength, state->phraseOffset,
                                            state->postingList);

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
        size_t collected = CollectLocations(backgroundState->cursor.get(), phrase, prefixLength,
                                            backgroundState->phraseOffset, backgroundState->postingList);
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

size_t Collector::CollectLocations(PrefixCursor *cursor, const vector<wid_t> &phrase,
                                   length_t prefixLength, size_t offset, shared_ptr<PostingList> &postingList) {
    if (offset == 0)
        assert(postingList == NULL);

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
                shared_ptr<PostingList> successors;
                CollectPhraseLocations(cursor, phrase, start, prefixLength, successors);

                postingList->Retain(successors.get(), start);
            }

            if (postingList->empty())
                break;

            start += prefixLength;
        }
    }

    return postingList == NULL ? 0 : postingList->size();
}

void Collector::CollectPhraseLocations(PrefixCursor *cursor, const vector<wid_t> &phrase, size_t offset, size_t length,
                                       shared_ptr<PostingList> &postingList) {
    if (postingList == NULL)
        postingList.reset(new PostingList());

    for (cursor->Seek(phrase, offset, length); cursor->HasNext(); cursor->Next())
        cursor->CollectValue(postingList.get());
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