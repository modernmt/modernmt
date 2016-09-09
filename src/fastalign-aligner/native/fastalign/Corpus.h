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
            Corpus(const string &sourceFilePath, const string &targetFilePath) : sourcePath(sourceFilePath),
                                                                                 targetPath(targetFilePath) {};

        private:
            const string sourcePath;
            const string targetPath;
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
