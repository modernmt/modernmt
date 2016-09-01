//
// Created by Davide  Caroselli on 23/08/16.
//

#include "Corpus.h"
#include <sstream>

using namespace fastalign;

Corpus::Corpus(const string &sourceFilePath, const string &targetFilePath) : sourcePath(sourceFilePath),
                                                                             targetPath(targetFilePath) {
}

void Corpus::ParseLine(const string &line, vector<word> &output) {
    output.clear();

    std::stringstream stream(line);
    word word;

    while (stream >> word)
        output.push_back(word);
}

CorpusReader::CorpusReader(const Corpus &corpus) : drained(false), source(corpus.sourcePath.c_str()),
                                                   target(corpus.targetPath.c_str()) {
}

bool CorpusReader::Read(sentence &outSource, sentence &outTarget) {
    string sourceLine, targetLine;
    if (!ReadLine(sourceLine, targetLine))
        return false;

    Corpus::ParseLine(sourceLine, outSource);
    Corpus::ParseLine(targetLine, outTarget);

    return true;
}

bool CorpusReader::Read(vector<pair<sentence, sentence>> &outBuffer, size_t limit) {
    if (drained)
        return false;

    for (size_t i = 0; i < limit; ++i) {
        sentence source, target;

        if (!Read(source, target))
            break;

        outBuffer.push_back(pair<sentence, sentence>(source, target));
    }

    return true;
}

bool CorpusReader::ReadLine(string &outSource, string &outTarget) {
    if (drained)
        return false;

    if (!getline(source, outSource) || !getline(target, outTarget)) {
        drained = true;
        return false;
    }

    return true;
}

bool CorpusReader::ReadLines(vector<pair<string, string>> &outBuffer, size_t limit) {
    if (drained)
        return false;

    for (size_t i = 0; i < limit; ++i) {
        string source, target;

        if (!ReadLine(source, target))
            break;

        outBuffer.push_back(pair<string, string>(source, target));
    }

    return true;
}
