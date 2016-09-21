//
// Created by Davide  Caroselli on 15/09/16.
//

#ifndef ROCKSLM_CACHEDROCKSLM_H
#define ROCKSLM_CACHEDROCKSLM_H

#include "LM.h"
#include "Options.h"
#include "RocksLM.h"
#include <cstdint>
#include <vector>
#include <string>

using namespace std;

namespace rockslm {

    class CachedRocksLM : public LM {
    public:
        CachedRocksLM(const RocksLM *lm, uint8_t cacheOrder = 3);

        ~CachedRocksLM();

        virtual float ComputeProbability(const wid_t word, const HistoryKey *historyKey,
                                         const context_t *context, HistoryKey **outHistoryKey) const override;

        virtual HistoryKey *MakeHistoryKey(const vector<wid_t> &phrase) const override;

        virtual HistoryKey *MakeEmptyHistoryKey() const override;

        virtual bool IsOOV(const context_t *context, const wid_t word) const override;

    private:
        RocksLM *lm;
        void *cache;
    };

}

#endif //ROCKSLM_CACHEDROCKSLM_H
