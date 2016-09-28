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

        struct translationOptions{
            size_t numScoreComponent;
            std::vector<wid_t> targetWords;
            std::vector<float> scores;
            alignment_t alignments;

            translationOptions(size_t components){
                numScoreComponent = components;
                scores.resize(numScoreComponent);
            }
        };

        class PhraseTable : public IncrementalModel {
        public:
            PhraseTable(const string &modelPath, const Options &options = Options());

            ~PhraseTable();

            virtual void Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                             const std::vector<wid_t> &target, const alignment_t &alignment) override;

            virtual vector<updateid_t> GetLatestUpdatesIdentifier() override;

            virtual void NormalizeContextMap(context_t *context);

            void GetTargetPhraseCollection(const std::vector<wid_t>& source_phrase, std::vector<mmt::sapt::translationOptions> *target_options) { return; };

        private:
            struct pt_private;
            pt_private *self;
        };

    }
}


#endif //SAPT_PHRASETABLE_H
