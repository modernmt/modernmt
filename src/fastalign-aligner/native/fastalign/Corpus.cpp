//
// Created by Davide  Caroselli on 23/08/16.
//

#include "Corpus.h"

using namespace mmt;
using namespace mmt::fastalign;

CorpusReader::CorpusReader(const Corpus &corpus) : drained(false), source(corpus.sourcePath.c_str()),
                                                   target(corpus.targetPath.c_str()) {
}

bool CorpusReader::Read(vector<wid_t> &outSource, vector<wid_t> &outTarget) {
    string sourceLine, targetLine;
    if (!ReadLine(sourceLine, targetLine))
        return false;

    ParseLine(sourceLine, outSource);
    ParseLine(targetLine, outTarget);

    return true;
}

bool CorpusReader::Read(vector<pair<vector<wid_t>, vector<wid_t>>> &outBuffer, size_t limit) {
    if (drained)
        return false;

    for (size_t i = 0; i < limit; ++i) {
        vector<wid_t> source, target;

        if (!Read(source, target))
            break;

        outBuffer.push_back(pair<vector<wid_t>, vector<wid_t>>(source, target));
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
