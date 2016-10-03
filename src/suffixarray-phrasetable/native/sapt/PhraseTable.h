//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_PHRASETABLE_H
#define SAPT_PHRASETABLE_H

#include <set>

#include <mmt/IncrementalModel.h>
//#include <suffixarray/SuffixArray.h>
#include "Options.h"

using namespace std;

namespace mmt {
    namespace sapt {

        struct TranslationOption {
            vector<wid_t> targetPhrase;
            alignment_t alignment;
            vector<float> scores;

            TranslationOption(size_t scoreSize) {
                scores.resize(scoreSize);
            }
        };

        class PhraseTable : public IncrementalModel {
        public:
            PhraseTable(const string &modelPath, const Options &options = Options());

            ~PhraseTable();

            void NormalizeContext(context_t *context);

            void GetTargetPhraseCollection(const vector<wid_t> &phrase, vector<TranslationOption> &outOptions, context_t *context);

            // TODO: just for testing purpose, must be removed asap
            void *__GetSuffixArray();

            /* IncrementalModel */

            virtual void Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                             const std::vector<wid_t> &target, const alignment_t &alignment) override;

            virtual vector<updateid_t> GetLatestUpdatesIdentifier() override;

        private:
            struct pt_private;
            pt_private *self;
            size_t numScoreComponent;

            void GetTranslationOptions(const vector<wid_t> &sourcePhrase,
                                       const std::vector<wid_t> &sourceSentence,
                                       const std::vector<wid_t> &targetSentence,
                                       const alignment_t &alignment,
                                       const std::vector<length_t> &offsets,
                                       std::vector<TranslationOption> &outOptions);

            void ExtractPhrasePairs(const std::vector<wid_t> &sourceSentence,
                                    const std::vector<wid_t> &targetSentence,
                                    const alignment_t &alignment,
                                    const std::vector<bool> &targetAligned,
                                    length_t sourceStart, length_t sourceEnd, int targetStart, int targetEnd,
                                    std::vector<TranslationOption> &outOptions);

        };
    }
}


#endif //SAPT_PHRASETABLE_H
