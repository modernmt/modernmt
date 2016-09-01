//
// Created by Davide  Caroselli on 23/08/16.
//

#ifndef FASTALIGN_CORPUS_H
#define FASTALIGN_CORPUS_H

#include <string>
#include <fstream>
#include "Model.h"

using namespace std;

namespace fastalign {

    class Corpus {
        friend class CorpusReader;
    public:
        Corpus(const string &sourceFilePath, const string &targetFilePath);

        static void ParseLine(const string &line, vector<word> &output);

    private:
        const string sourcePath;
        const string targetPath;
    };

    class CorpusReader {
    public:
        CorpusReader(const Corpus &corpus);

        bool Read(sentence &outSource, sentence &outTarget);

        bool Read(vector<pair<sentence, sentence>> &outBuffer, size_t limit);

        bool ReadLine(string &outSource, string &outTarget);

        bool ReadLines(vector<pair<string, string>> &outBuffer, size_t limit);

    private:
        bool drained;

        std::ifstream source;
        std::ifstream target;
    };

}


#endif //FASTALIGN_CORPUS_H
