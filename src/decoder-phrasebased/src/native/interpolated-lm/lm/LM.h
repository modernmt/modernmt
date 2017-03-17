//
// Created by Davide Caroselli on 27/07/16.
//

#ifndef ILM_LM_H
#define ILM_LM_H

#include <cstdint>
#include <vector>
#include <map>
#include <mmt/sentence.h>

using namespace std;

namespace mmt {
    namespace ilm {

        struct HistoryKey {
            virtual size_t hash() const = 0;

            virtual bool operator==(const HistoryKey &other) const = 0;

            virtual size_t length() const = 0;

            virtual ~HistoryKey() {};
        };

        const wid_t kVocabularyStartSymbol = 1;
        const wid_t kVocabularyEndSymbol = 2;

        class LM {
        public:

            // Returned value must be le natural log of the ngram probability
            virtual float ComputeProbability(const wid_t word, const HistoryKey *historyKey, const context_t *context,
                                             HistoryKey **outHistoryKey) const = 0;

            virtual HistoryKey *MakeHistoryKey(const vector <wid_t> &phrase) const = 0;

            virtual HistoryKey *MakeEmptyHistoryKey() const = 0;

            virtual bool IsOOV(const context_t *context, const wid_t word) const = 0;
        };

    }
}


#endif //ILM_LM_H
