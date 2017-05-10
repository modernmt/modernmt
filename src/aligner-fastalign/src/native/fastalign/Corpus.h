//
// Created by Davide  Caroselli on 23/08/16.
//

#ifndef FASTALIGN_CORPUS_H
#define FASTALIGN_CORPUS_H

#include <string>
#include <vector>
#include <fstream>
#include <sstream>
#include <mmt/sentence.h>

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

            bool Read(std::vector<std::string> &outSource, std::vector<std::string> &outTarget);

            bool Read(std::vector<std::pair<std::vector<std::string>, std::vector<std::string>>> &outBuffer,
                      size_t limit);

            bool Read(std::vector<wid_t> &outSource, std::vector<wid_t> &outTarget);

            bool Read(std::vector<std::pair<std::vector<wid_t>, std::vector<wid_t>>> &outBuffer,
                      size_t limit);

        private:
            bool drained;

            const Vocabulary *vocabulary;
            std::ifstream source;
            std::ifstream target;
        };
    }
}

#endif //FASTALIGN_CORPUS_H
