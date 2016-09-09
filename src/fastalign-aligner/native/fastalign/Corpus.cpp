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
    if (drained)
        return false;

    string sourceLine, targetLine;
    if (!getline(source, sourceLine) || !getline(target, targetLine)) {
        drained = true;
        return false;
    }

    ParseLine(sourceLine, outSource);
    ParseLine(targetLine, outTarget);

    return true;
}

bool CorpusReader::Read(vector<pair<vector<wid_t>, vector<wid_t>>> &outBuffer, size_t limit) {
    if (drained)
        return false;

    vector<pair<string, string>> batch;
    for (size_t i = 0; i < limit; ++i) {
        string sourceLine, targetLine;
        if (!getline(source, sourceLine) || !getline(target, targetLine)) {
            drained = true;
            break;
        }

        batch.push_back(pair<string, string>(sourceLine, targetLine));
    }

    if (batch.empty())
        return false;

    outBuffer.resize(batch.size());
#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < batch.size(); ++i) {
        ParseLine(batch[i].first, outBuffer[i].first);
        ParseLine(batch[i].second, outBuffer[i].second);
    }

    return true;
}
