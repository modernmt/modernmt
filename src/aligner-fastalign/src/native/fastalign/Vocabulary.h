//
// Created by Davide  Caroselli on 09/05/17.
//

#ifndef MMT_FASTALIGN_VOCABULARY_H
#define MMT_FASTALIGN_VOCABULARY_H

#include <string>
#include <unordered_set>
#include "alignment.h"
#include "Corpus.h"

namespace mmt {
    namespace fastalign {

        static const word_t kNullWord = 0;
        static const word_t kUnknownWord = 1;

        static const std::string kEmptyResult = "";

        class Vocabulary {
        public:

            static const Vocabulary *FromCorpus(CorpusReader &reader, double threshold = 0.);

            Vocabulary(const std::string &filename, bool direct = true, bool reverse = true);

            void Store(const std::string &filename) const;

            inline const word_t Get(const std::string &term) const {
                auto result = vocab.find(term);
                return result == vocab.end() ? kUnknownWord : result->second;
            }

            inline const std::string &Get(word_t id) const {
                return (id > 1 && (id - 2) < terms.size()) ? terms[id - 2] : kEmptyResult;
            }

            inline const void Encode(const sentence_t &sentence, wordvec_t &output) const {
                output.resize(sentence.size());
                for (size_t i = 0; i < sentence.size(); ++i)
                    output[i] = Get(sentence[i]);
            }

        private:
            Vocabulary() {};

            std::vector<std::string> terms;
            std::unordered_map<std::string, word_t> vocab;
        };

    }
}

#endif //MMT_FASTALIGN_VOCABULARY_H
