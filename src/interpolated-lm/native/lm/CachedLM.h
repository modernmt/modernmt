//
// Created by Davide  Caroselli on 15/09/16.
//

#ifndef ILM_CACHEDLM_H
#define ILM_CACHEDLM_H

#include "LM.h"
#include "Options.h"
#include "InterpolatedLM.h"
#include <cstdint>
#include <vector>
#include <string>

using namespace std;

namespace mmt {
    namespace ilm {

        class CachedLM : public LM {
        public:
            CachedLM(const InterpolatedLM *lm, uint8_t cacheOrder = 3);

            ~CachedLM();

            virtual float ComputeProbability(const wid_t word, const HistoryKey *historyKey,
                                             const context_t *context, HistoryKey **outHistoryKey) const override;

            virtual HistoryKey *MakeHistoryKey(const vector <wid_t> &phrase) const override;

            virtual HistoryKey *MakeEmptyHistoryKey() const override;

            virtual bool IsOOV(const context_t *context, const wid_t word) const override;

        private:
            InterpolatedLM *lm;
            void *cache;
        };

    }
}

#endif //ILM_CACHEDLM_H
