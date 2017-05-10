//
// Created by Davide  Caroselli on 09/05/17.
//

#ifndef MMT_FASTALIGN_VOCABULARY_H
#define MMT_FASTALIGN_VOCABULARY_H

#include <string>
#include <unordered_set>
#include <mmt/sentence.h>
#include "Corpus.h"

namespace mmt {
    namespace fastalign {

        static const wid_t kNullWordId = 0;
        static const wid_t kUnknownWordId = 1;

        static const std::string kEmptyResult = "";

        class Vocabulary {
        public:

            static const Vocabulary *FromCorpus(const Corpus &corpus);

            Vocabulary(const std::string &filename, bool direct = true, bool reverse = true);

            void Store(const std::string &filename) const;

            inline const wid_t Get(const std::string &term) const {
                auto result = vocab.find(term);
                return result == vocab.end() ? kUnknownWordId : result->second;
            }

            inline const std::string &Get(wid_t id) const {
                return (id > 1 && (id - 2) < terms.size()) ? terms[id - 2] : kEmptyResult;
            }

        private:
            Vocabulary() {};

            std::vector<std::string> terms;
            std::unordered_map<std::string, wid_t> vocab;
        };

    }
}

#endif //MMT_FASTALIGN_VOCABULARY_H
