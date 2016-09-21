//
// Created by Davide  Caroselli on 15/09/16.
//

#include "CachedRocksLM.h"
#include "AdaptiveLMCache.h"

using namespace std;
using namespace rockslm;

CachedRocksLM::CachedRocksLM(const RocksLM *lm, uint8_t cacheOrder) : lm((RocksLM *) lm) {
    cache = new AdaptiveLMCache(cacheOrder);
}

CachedRocksLM::~CachedRocksLM() {
    delete (AdaptiveLMCache *) cache;
}

HistoryKey *CachedRocksLM::MakeHistoryKey(const vector<wid_t> &phrase) const {
    return lm->MakeHistoryKey(phrase);
}

HistoryKey *CachedRocksLM::MakeEmptyHistoryKey() const {
    return lm->MakeEmptyHistoryKey();
}

bool CachedRocksLM::IsOOV(const context_t *context, const wid_t word) const {
    return lm->IsOOV(context, word);
}

float CachedRocksLM::ComputeProbability(const wid_t word, const HistoryKey *historyKey, const context_t *context,
                                        HistoryKey **outHistoryKey) const {
    return lm->ComputeProbability(word, historyKey, context, outHistoryKey, cache);
}


