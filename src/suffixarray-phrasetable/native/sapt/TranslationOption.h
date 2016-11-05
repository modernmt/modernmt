//
// Created by Davide  Caroselli on 11/10/16.
//

#ifndef SAPT_TRANSLATIONOPTION_H
#define SAPT_TRANSLATIONOPTION_H

#include <vector>
#include <unordered_map>
#include <mmt/sentence.h>
#include "OrientationCounts.h"

using namespace std;

namespace mmt {
    namespace sapt {

        const size_t kTranslationOptionScoreCount = 4;
        const size_t kTOForwardProbability = 0;
        const size_t kTOBackwardProbability = 1;
        const size_t kTOForwardLexicalProbability = 2;
        const size_t kTOBackwardLexicalProbability = 3;

        struct TranslationOption {
            vector<wid_t> targetPhrase;
            alignment_t alignment;
            vector<float> scores;
            OrientationCounts orientationCounts;

            TranslationOption() {
                scores.resize(kTranslationOptionScoreCount, 0.f);
            }

            string ToString() const {
                ostringstream repr;
                for (auto w = targetPhrase.begin(); w != targetPhrase.end(); ++w)
                    repr << *w << " ";
                repr << " |||";
                for (auto o = scores.begin(); o != scores.end(); ++o)
                    repr << " " << *o;
                repr << " |||";
                for (auto a = alignment.begin(); a != alignment.end(); ++a)
                    repr << " " << a->first << "-" << a->second;
                repr << " |||";
                for (size_t i = 0; i < orientationCounts.ForwardOrientationCounts.size(); ++i)
                    repr << " " << orientationCounts.ForwardOrientationCounts[i];
                repr << " ";
                for (size_t i = 0; i < orientationCounts.BackwardOrientationCounts.size(); ++i)
                    repr << " " << orientationCounts.BackwardOrientationCounts[i];

                return repr.str();
            }

            bool operator==(const TranslationOption &rhs) const {
                return targetPhrase == rhs.targetPhrase;
            }

            bool operator!=(const TranslationOption &rhs) const {
                return !(rhs == *this);
            }

            const vector<float>& GetForwardOrientationCounts() const {
                return orientationCounts.ForwardOrientationCounts;
            }
            const vector<float>& GetBackwardOrientationCounts() const {
                return orientationCounts.BackwardOrientationCounts;
            }
        };
    }
}

#endif //SAPT_TRANSLATIONOPTION_H
