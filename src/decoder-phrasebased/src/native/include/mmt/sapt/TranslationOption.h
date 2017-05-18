//
// Created by Davide  Caroselli on 11/10/16.
//

#ifndef SAPT_TRANSLATIONOPTION_H
#define SAPT_TRANSLATIONOPTION_H

#include <vector>
#include <unordered_map>
#include <mmt/sentence.h>

using namespace std;

namespace mmt {
    namespace sapt {

        const size_t kTranslationOptionDistortionCount = 4;
        const size_t kTranslationOptionScoreCount = 4;

        enum Score {
            ForwardProbabilityScore = 0,
            BackwardProbabilityScore = 1,
            ForwardLexicalScore = 2,
            BackwardLexicalScore = 3,
        };

        enum Orientation {
            MonotonicOrientation = 0,
            SwapOrientation = 1,
            DiscontinuousLeftOrientation = 2,
            DiscontinuousRightOrientation = 3,
            NoOrientation = 4
        };

        struct TranslationOption {

            struct Orientations {
                vector<size_t> forward;
                vector<size_t> backward;

                Orientations() {
                    forward.resize(kTranslationOptionDistortionCount + 1, 0); // Including NoOrientation
                    backward.resize(kTranslationOptionDistortionCount + 1, 0); // Including NoOrientation
                };

                string ToString() const {
                    ostringstream repr;
                    repr << "forward: " << OrientationToString(forward) << " ";
                    repr << "backward: " << OrientationToString(backward);
                    return repr.str();
                }

                string OrientationToString(const vector<size_t> &orientation) const {
                    ostringstream repr;
                    repr << "M=" << orientation[MonotonicOrientation] << " ";
                    repr << "S=" << orientation[SwapOrientation] << " ";
                    repr << "DL=" << orientation[DiscontinuousLeftOrientation] << " ";
                    repr << "DR=" << orientation[DiscontinuousRightOrientation] << " ";
                    repr << "N=" << orientation[NoOrientation];
                    return repr.str();
                }

                void AddToForward(Orientation orientation, size_t count = 1) {
                    forward[orientation] += count;
                }

                void AddToBackward(Orientation orientation, size_t count = 1) {
                    backward[orientation] += count;
                }
            };

            vector<wid_t> targetPhrase;
            alignment_t alignment;
            Orientations orientations;
            vector<float> scores;

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

                repr << " ||| " << orientations.ToString();

                return repr.str();
            }

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
