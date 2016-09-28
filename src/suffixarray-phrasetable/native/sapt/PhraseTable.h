//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_PHRASETABLE_H
#define SAPT_PHRASETABLE_H

#include <set>

#include <mmt/IncrementalModel.h>
#include "Options.h"

using namespace std;

namespace mmt {
    namespace sapt {

        struct TranslationOption {
            size_t numScoreComponent;
            vector<wid_t> targetWords;
            vector<float> scores;
            alignment_t alignments;

            TranslationOption(size_t components) {
                numScoreComponent = components;
                scores.resize(numScoreComponent);
            }
        };

        class PhraseTable : public IncrementalModel {
        public:
            PhraseTable(const string &modelPath, const Options &options = Options());

            ~PhraseTable();

            void NormalizeContextMap(context_t *context) {
                // TODO: stub implementation (do nothing)
            }

            void GetTargetPhraseCollection(const vector<wid_t> &phrase, vector<TranslationOption> *outOptions) {
                // TODO: stub implementation (do nothing)
            };

            // TODO: just for testing purpose, must be removed asap
            void *__GetSuffixArray();

            /* IncrementalModel */

            virtual void Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                             const std::vector<wid_t> &target, const alignment_t &alignment) override;

            virtual vector<updateid_t> GetLatestUpdatesIdentifier() override;

        private:
            struct pt_private;
            pt_private *self;
        };

    }
}


#endif //SAPT_PHRASETABLE_H
