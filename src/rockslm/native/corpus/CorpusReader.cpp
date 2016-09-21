//
// Created by Davide  Caroselli on 08/09/16.
//

#include "CorpusReader.h"
#include <fstream>

using namespace rockslm;

struct noop {
    void operator()(...) const {}
};

CorpusReader::CorpusReader(const string &corpus) : drained(false) {
    input.reset(new ifstream(corpus.c_str()));
}

CorpusReader::CorpusReader(istream *stream) : drained(false) {
    input.reset(stream, noop());
}

bool CorpusReader::Read(vector<wid_t> &outSentence) {
    if (drained)
        return false;

    string line;
    if (!getline(*input, line)) {
        drained = true;
        return false;
    }

    ParseLine(line, outSentence);

    return true;
}
