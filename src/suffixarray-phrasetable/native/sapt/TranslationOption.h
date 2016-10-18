//
// Created by Davide  Caroselli on 11/10/16.
//

#ifndef SAPT_TRANSLATIONOPTION_H
#define SAPT_TRANSLATIONOPTION_H

#include <cassert>
#include <vector>
#include <unordered_map>
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
            unordered_map<alignment_t, size_t, alignment_hash> alignments;
            vector<float> scores;

            TranslationOption() {
                scores.resize(kTranslationOptionScoreCount, 0.f);
            }

            void SetBestAlignment() { //the best alignment has the larger count, and, in case of equality, the first
                assert (!alignments.empty());
                auto best_entry = alignments.begin();
                size_t best_count = 0;
                for (auto entry = alignments.begin(); entry != alignments.end(); ++entry) {
                    if (best_count < entry->second) {
                        best_count = entry->second;
                        best_entry = entry;
                    }
                }
                alignment = best_entry->first;
            }

            //insert a new alignment for the current options, increased its count by one if required
            void InsertAlignment(alignment_t &alignment){
                auto ptr = alignments.find(alignment);
                if (ptr != alignments.end())
                    ptr->second = ptr->second + 1;
                else
                    alignments[alignment] = 1;
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
