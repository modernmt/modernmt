//
// Created by Davide  Caroselli on 08/09/16.
//

#include "CorpusReader.h"
#include <fstream>

using namespace mmt;
using namespace mmt::ilm;

struct noop {
    void operator()(...) const {}
};

inline void ParseLine(Vocabulary &vb, const string &line, vector<wid_t> &output) {
    output.clear();

    std::stringstream stream(line);
    string word;

    while (stream >> word)
        output.push_back(vb.Lookup(word, false));
}

CorpusReader::CorpusReader(Vocabulary &vocabulary, const string &corpus) : drained(false), vb(vocabulary) {
    input.reset(new ifstream(corpus.c_str()));
}

CorpusReader::CorpusReader(Vocabulary &vocabulary, istream *stream) : drained(false), vb(vocabulary) {
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

    ParseLine(vb, line, outSentence);

    return true;
}
