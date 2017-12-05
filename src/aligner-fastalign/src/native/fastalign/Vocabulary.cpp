//
// Created by Davide  Caroselli on 09/05/17.
//

#include "Vocabulary.h"
#include <iostream>
#include <unordered_map>

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

const Vocabulary *Vocabulary::FromCorpus(const Corpus &corpus) {
    // For model efficiency all source words have the lowest id possible
    unordered_set<string> src_terms;
    unordered_set<string> trg_terms;

    unordered_map<string,size_t> src_terms_map;
    unordered_map<string,size_t> trg_terms_map;

    CorpusReader reader(corpus);
    vector<string> src, trg;

    size_t lines = 0;
    while (reader.Read(src, trg)) {
        for (auto w=src.begin(); w!=src.end(); ++w)
            if (src_terms_map.find(*w) != src_terms_map.end())
                ++src_terms_map[*w];
            else
                src_terms_map[*w] = 1;

        for (auto w=trg.begin(); w!=trg.end(); ++w)
            if (trg_terms_map.find(*w) != trg_terms_map.end())
                ++trg_terms_map[*w];
            else
                trg_terms_map[*w] = 1;
        ++lines;
    }

    std::cerr << std::endl << "Corpus size after removal of empty segments:" << lines << std::endl;

    for (auto e=src_terms_map.begin(); e!=src_terms_map.end(); ++e){
        if (e->second > vocab_threshold)
            src_terms.insert(e->first);
    }
    for (auto e=trg_terms_map.begin(); e!=trg_terms_map.end(); ++e){
        if (e->second > vocab_threshold)
            trg_terms.insert(e->first);
    }
    std::cerr << "Source vocabulary size:" << src_terms.size() << " (before pruning of singletons:" << src_terms_map.size() << ")" << std::endl;
    std::cerr << "Target vocabulary size:" << trg_terms.size() << " (before pruning of singletons:" << trg_terms_map.size() << ")" << std::endl;

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