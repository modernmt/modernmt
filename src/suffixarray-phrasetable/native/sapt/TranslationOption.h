//
// Created by Davide  Caroselli on 11/10/16.
//

#ifndef SAPT_TRANSLATIONOPTION_H
#define SAPT_TRANSLATIONOPTION_H

#include <vector>
#include <mmt/sentence.h>

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

            TranslationOption() {
                scores.resize(kTranslationOptionScoreCount, 0.f);
            }

            string ToString() const {
                ostringstream repr;
                for (auto w = targetPhrase.begin(); w != targetPhrase.end(); ++w)
                    repr << *w << " ";
                repr << " |||";
                for (auto a = alignment.begin(); a != alignment.end(); ++a)
                    repr << " " << a->first << "-" << a->second;
                repr << " |||";
                for (auto o = scores.begin(); o != scores.end(); ++o)
                    repr << " " << *o;

                return repr.str();
            }

            struct hash {
                size_t operator()(const TranslationOption &x) const {
                    size_t h = 0;
                    for (size_t i = 0; i < x.targetPhrase.size(); ++i) {
                        wid_t word = x.targetPhrase[i];

                        if (i == 0)
                            h = word;
                        else
                            h = ((h * 8978948897894561157ULL) ^
                                 (static_cast<uint64_t>(1 + word) * 17894857484156487943ULL));
                    }

                    return h;
                }
            };

            bool operator==(const TranslationOption &rhs) const {
                return targetPhrase == rhs.targetPhrase;
            }

            bool operator!=(const TranslationOption &rhs) const {
                return !(rhs == *this);
            }

        };

    }
}

#endif //SAPT_TRANSLATIONOPTION_H
