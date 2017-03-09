//
// Created by Davide  Caroselli on 07/09/16.
//

#ifndef ILM_STATICLM_H
#define ILM_STATICLM_H

#include <lm/model.hh>
#include "LM.h"

namespace mmt {
    namespace ilm {

        class StaticLM : public LM {
        public:

            StaticLM(const string &modelPath);

            ~StaticLM();

            virtual float ComputeProbability(const wid_t word, const HistoryKey *historyKey,
                                             const context_t *context, HistoryKey **outHistoryKey) const override;

            virtual HistoryKey *MakeHistoryKey(const vector <wid_t> &phrase) const override;

            virtual HistoryKey *MakeEmptyHistoryKey() const override;

            virtual bool IsOOV(const context_t *context, const wid_t word) const override;

        private:
            lm::ngram::Model *model;
        };

    }
}


#endif //ILM_STATICLM_H
