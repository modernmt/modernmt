//
// Created by Davide  Caroselli on 10/10/16.
//

#ifndef SAPT_COLLECTOR_H
#define SAPT_COLLECTOR_H

#include <mmt/sentence.h>
#include "PrefixCursor.h"
#include "sample.h"
#include "CorpusStorage.h"

namespace mmt {
    namespace sapt {

        class Collector {
            friend class SuffixArray;

        public:

            inline void Extend(wid_t word, size_t limit, vector<sample_t> &outSamples) {
                vector<wid_t> words(1);
                words[0] = word;

                Extend(words, limit, outSamples);
            }

            void Extend(const vector<wid_t> &words, size_t limit, vector<sample_t> &outSamples);

        private:
            Collector(CorpusStorage *storage, rocksdb::DB *db, length_t prefixLength, const context_t *context,
                      bool searchInBackground);

            void Retrieve(const vector<location_t> &locations, vector<sample_t> &outSamples);

            static size_t CollectLocations(PrefixCursor *cursor, const vector<wid_t> &phrase, length_t prefixLength,
                                           size_t offset = 0, PostingList **postingList = NULL);

            static inline void CollectPhraseLocations(PrefixCursor *cursor, const vector<wid_t> &phrase,
                                                      size_t offset, size_t length, PostingList **postingList);

            ~Collector() {
                if (backgroundState) delete backgroundState;
            }

            struct state_t {
                size_t phraseOffset;
                PrefixCursor *cursor;
                PostingList *postingList;

                state_t() : phraseOffset(0), cursor(NULL), postingList(NULL) {};

                ~state_t() {
                    if (cursor) delete cursor;
                    if (postingList) delete postingList;
                }
            };

            const length_t prefixLength;
            const CorpusStorage *storage;

            vector<wid_t> phrase;
            vector<state_t> inDomainStates;
            state_t *backgroundState = NULL;
        };

    }
}


#endif //SAPT_COLLECTOR_H
