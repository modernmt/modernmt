//
// Created by Davide Caroselli on 27/07/16.
//

#ifndef ROCKSLM_ROCKSLM_H
#define ROCKSLM_ROCKSLM_H

#include "LM.h"
#include "Options.h"
#include <mmt/IncrementalModel.h>
#include <cstdint>
#include <vector>
#include <string>

using namespace mmt;
using namespace std;

namespace rockslm {

    class RocksLM : public LM, public IncrementalModel {
        friend class CachedRocksLM;

    public:
        RocksLM(const string &modelPath, const Options &options = Options());

        ~RocksLM();

        /* LM */

        inline virtual float ComputeProbability(const wid_t word, const HistoryKey *historyKey,
                                                const context_t *context, HistoryKey **outHistoryKey) const override {
            return ComputeProbability(word, historyKey, context, outHistoryKey, NULL);
        }

        virtual HistoryKey *MakeHistoryKey(const vector<wid_t> &phrase) const override;

        virtual HistoryKey *MakeEmptyHistoryKey() const override;

        virtual bool IsOOV(const context_t *context, const wid_t word) const override;

        /* Incremental Model */

        virtual void Add(const updateid_t &id, const domain_t domain,
                         const vector<wid_t> &source, const vector<wid_t> &target, const alignment_t &alignment) override;

        virtual vector<updateid_t> GetLatestUpdatesIdentifier() override;

        virtual void NormalizeContextMap(context_t *context) {
            void self->alm->NormalizeContextMap(context);
        };

    private:
        struct rockslm_private;
        rockslm_private *self;

        float ComputeProbability(const wid_t word, const HistoryKey *historyKey,
                                 const context_t *context, HistoryKey **outHistoryKey, void *cache) const;
    };

}

#endif //ROCKSLM_ROCKSLM_H
