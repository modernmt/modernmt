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
            Corpus(const std::string &sourceFilePath, const std::string &targetFilePath,
                   const std::string &outputFilePath = "") : sourcePath(sourceFilePath), targetPath(targetFilePath),
                                                             outputPath(outputFilePath) {};

            static void List(const std::string &path, const std::string &outPath,
                             const std::string &sourceLang, const std::string &targetLang, std::vector<Corpus> &list);

            const std::string &GetOutputPath() const {
                return outputPath;
            }

        private:
            const std::string sourcePath;
            const std::string targetPath;
            const std::string outputPath;
        };

        class CorpusReader {
        public:
            CorpusReader(const Corpus &corpus, const Vocabulary *vocabulary = nullptr);

            bool Read(sentence_t &outSource, sentence_t &outTarget);

            bool Read(std::vector<std::pair<sentence_t, sentence_t>> &outBuffer, size_t limit);

            bool Read(wordvec_t &outSource, wordvec_t &outTarget);

            bool Read(std::vector<std::pair<wordvec_t, wordvec_t>> &outBuffer, size_t limit);

        private:
            bool drained;

            const Vocabulary *vocabulary;
            std::ifstream source;
            std::ifstream target;
        };
    }
}

#endif //FASTALIGN_CORPUS_H
