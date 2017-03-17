//
// Created by Davide  Caroselli on 15/09/16.
//

#include "CachedLM.h"
#include "AdaptiveLMCache.h"

using namespace std;
using namespace mmt::ilm;

CachedLM::CachedLM(const InterpolatedLM *lm, uint8_t cacheOrder) : lm((InterpolatedLM *) lm) {
    cache = new AdaptiveLMCache(cacheOrder);
}

CachedLM::~CachedLM() {
    delete (AdaptiveLMCache *) cache;
}

HistoryKey *CachedLM::MakeHistoryKey(const vector<wid_t> &phrase) const {
    return lm->MakeHistoryKey(phrase);
}

HistoryKey *CachedLM::MakeEmptyHistoryKey() const {
    return lm->MakeEmptyHistoryKey();
}

bool CachedLM::IsOOV(const context_t *context, const wid_t word) const {
    return lm->IsOOV(context, word);
}

float CachedLM::ComputeProbability(const wid_t word, const HistoryKey *historyKey, const context_t *context,
                                        HistoryKey **outHistoryKey) const {
    return lm->ComputeProbability(word, historyKey, context, outHistoryKey, cache);
}


