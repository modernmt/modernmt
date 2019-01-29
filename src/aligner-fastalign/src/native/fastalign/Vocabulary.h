//
// Created by Davide  Caroselli on 09/05/17.
//

#ifndef MMT_FASTALIGN_VOCABULARY_H
#define MMT_FASTALIGN_VOCABULARY_H

#include <string>
#include <unordered_set>
#include "alignment.h"
#include "Corpus.h"
#include <boost/locale.hpp>
#include <boost/locale/generator.hpp>

namespace mmt {
    namespace fastalign {

        static const word_t kNullWord = 0;
        static const word_t kUnknownWord = 1;

        class Vocabulary {
        public:

            static const Vocabulary *BuildFromCorpora(const std::vector<Corpus> &corpora, const std::string &filename,
                                                      size_t maxLineLength = 0, bool case_sensitive = true,
                                                      double threshold = 0.);

            explicit Vocabulary(const std::string &filename);

            inline const size_t Size() const {
                return vocab.size() + 2;
            }

            inline const word_t Get(const std::string &term) const {
                auto result = vocab.find(case_sensitive ? term : boost::locale::to_lower(term, locale));
                return result == vocab.end() ? kUnknownWord : result->second;
            }

            inline const void Encode(const sentence_t &sentence, wordvec_t &output) const {
                output.resize(sentence.size());
                for (size_t i = 0; i < sentence.size(); ++i)
                    output[i] = Get(sentence[i]);
            }

            inline const score_t GetProbability(const std::string &term, bool is_source) const {
                return GetProbability(Get(term), is_source);
            }

            inline const score_t GetProbability(word_t id, bool is_source) const {
                if (id > 1 && (id - 2) < probs.size()) {
                    const std::pair<score_t, score_t> &pair = probs[id - 2];
                    return is_source ? pair.first : pair.second;
                } else {
                    return 0;
                }
            }

        private:
            std::locale locale;
            bool case_sensitive;
            std::vector<std::pair<score_t, score_t>> probs;
            std::unordered_map<std::string, word_t> vocab;
        };

    }
}

#endif //MMT_FASTALIGN_VOCABULARY_H
