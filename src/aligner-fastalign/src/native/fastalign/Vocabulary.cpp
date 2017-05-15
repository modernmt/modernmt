//
// Created by Davide  Caroselli on 09/05/17.
//

#include "Vocabulary.h"
#include <iostream>

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

const Vocabulary *Vocabulary::FromCorpus(const Corpus &corpus) {
    // For model efficiency all source words have the lowest id possible
    unordered_set<string> src_terms;
    unordered_set<string> trg_terms;

    CorpusReader reader(corpus);
    vector<string> src, trg;

    while (reader.Read(src, trg)) {
        src_terms.insert(src.begin(), src.end());
        trg_terms.insert(trg.begin(), trg.end());
    }

    for (auto term = src_terms.begin(); term != src_terms.end(); ++term)
        trg_terms.erase(*term);

    Vocabulary *result = new Vocabulary();
    result->terms.insert(result->terms.end(), src_terms.begin(), src_terms.end());
    result->terms.insert(result->terms.end(), trg_terms.begin(), trg_terms.end());

    for (word_t id = 0; id < result->terms.size(); ++id)
        result->vocab[result->terms[id]] = (id + 2);

    return result;
}

Vocabulary::Vocabulary(const std::string &filename, bool direct, bool reverse) {
    ifstream input(filename);

    word_t index = 2;
    for (std::string line; getline(input, line); ++index) {
        if (direct)
            vocab[line] = index;

        if (reverse)
            terms.push_back(line);
    }
}

void Vocabulary::Store(const std::string &filename) const {
    ofstream output(filename);

    for (auto term = terms.begin(); term != terms.end(); ++term)
        output << (*term) << '\n';
}