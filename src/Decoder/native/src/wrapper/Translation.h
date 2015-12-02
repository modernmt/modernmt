//
// Created by Davide  Caroselli on 01/12/15.
//

#ifndef JNIMOSES_TRANSLATION_H
#define JNIMOSES_TRANSLATION_H

#include <string>
#include <vector>
#include "TranslationHypothesis.h"

namespace JNIWrapper {
    class Translation {
        int64_t session;
        std::string text;
        std::vector<TranslationHypothesis> hypotheses;
    public:

        Translation() : session(0), text(), hypotheses() { }

        void setText(const std::string &text) {
            Translation::text = text;
        }

        void setSession(int64_t session) {
            Translation::session = session;
        }

        const std::string &getText() const {
            return text;
        }

        std::vector<TranslationHypothesis> &getHypotheses() {
            return hypotheses;
        }

        int64_t getSession() const {
            return session;
        }
    };
}


#endif //JNIMOSES_TRANSLATION_H
