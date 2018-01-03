//
// Created by Davide  Caroselli on 23/08/16.
//

#ifndef FASTALIGN_CORPUS_H
#define FASTALIGN_CORPUS_H

#include <string>
#include <vector>
#include <fstream>
#include <sstream>
#include "alignment.h"

namespace mmt {
    namespace fastalign {

        class Vocabulary;

        class Corpus {
            friend class CorpusReader;

        public:
            Corpus(const std::string &sourceFile, const std::string &targetFile);

            static void List(const std::string &path, const std::string &sourceLang, const std::string &targetLang,
                             std::vector<Corpus> &outList);

            const std::string &GetName() const {
                return name;
            }

        private:
            const std::string name;
            const std::string sourceFile;
            const std::string targetFile;
        };

        class CorpusReader {
        public:
            CorpusReader(const Corpus &corpus, const Vocabulary *vocabulary = nullptr,
                         const size_t maxLineLength = 0, const bool skipEmptyLines = false);

            bool Read(sentence_t &outSource, sentence_t &outTarget);

            bool Read(std::vector<std::pair<sentence_t, sentence_t>> &outBuffer, size_t limit);

            bool Read(wordvec_t &outSource, wordvec_t &outTarget);

            bool Read(std::vector<std::pair<wordvec_t, wordvec_t>> &outBuffer, size_t limit);

        private:
            bool drained;

            const Vocabulary *vocabulary;
            std::ifstream source;
            std::ifstream target;

            const size_t maxLineLength;
            const bool skipEmptyLines;

            inline bool Skip(const sentence_t &source, const sentence_t &target) const {
                if (skipEmptyLines && (source.empty() || target.empty()))
                    return true;

                return maxLineLength > 0 && (source.size() > maxLineLength || target.size() > maxLineLength);
            }

            inline bool Skip(const wordvec_t &source, const wordvec_t &target) const {
                if (skipEmptyLines && (source.empty() || target.empty()))
                    return true;

                return maxLineLength > 0 && (source.size() > maxLineLength || target.size() > maxLineLength);
            }
        };
    }
}

#endif //FASTALIGN_CORPUS_H
