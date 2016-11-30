//
// Created by Davide  Caroselli on 23/08/16.
//

#ifndef FASTALIGN_CORPUS_H
#define FASTALIGN_CORPUS_H

#include <string>
#include <fstream>
#include <sstream>
#include <mmt/sentence.h>

using namespace std;

namespace mmt {
    namespace fastalign {

        class Corpus {
            friend class CorpusReader;

        public:
            Corpus(const string &sourceFilePath, const string &targetFilePath, const string &outputFilePath = "") : sourcePath(sourceFilePath),
                                                                                 targetPath(targetFilePath),
                                                                                 outputPath(outputFilePath) {};


            static void
                    List(const string &path, const string &outPath, const string &sourceLang, const string &targetLang, vector<Corpus> &list);

            const string &getOutputPath() const {
                return outputPath;
            }

            const string &getCorpusName() const;

        private:
            const string sourcePath;
            const string targetPath;
            const string outputPath;
        };

        class CorpusReader {
        public:
            CorpusReader(const Corpus &corpus);

            bool Read(vector<wid_t> &outSource, vector<wid_t> &outTarget);

            bool Read(vector<pair<vector<wid_t>, vector<wid_t>>> &outBuffer, size_t limit);

        private:
            bool drained;

            ifstream source;
            ifstream target;

            static inline void ParseLine(const string &line, vector<wid_t> &output) {
                output.clear();

                std::stringstream stream(line);
                wid_t word;

                while (stream >> word)
                    output.push_back(word);
            }
        };

    }
}

#endif //FASTALIGN_CORPUS_H
