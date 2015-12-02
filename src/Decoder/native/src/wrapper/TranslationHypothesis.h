//
// Created by Davide  Caroselli on 01/12/15.
//

#ifndef JNIMOSES_TRANSLATIONHYPOTHESIS_H
#define JNIMOSES_TRANSLATIONHYPOTHESIS_H

#include <string>
#include <vector>
#include "Feature.h"

namespace JNIWrapper {
    class TranslationHypothesis {
        std::string text;
        float totalScore;
        std::vector<Feature> scores;

    public:

        TranslationHypothesis() : text(), totalScore(0.f), scores() { }

        void setText(const std::string &text) {
            TranslationHypothesis::text = text;
        }

        void setTotalScore(float totalScore) {
            TranslationHypothesis::totalScore = totalScore;
        }

        const std::string &getText() const {
            return text;
        }

        float getTotalScore() const {
            return totalScore;
        }

        std::vector<Feature> &getScores() {
            return scores;
        }
    };
}


#endif //JNIMOSES_TRANSLATIONHYPOTHESIS_H
