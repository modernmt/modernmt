//
// Created by Davide  Caroselli on 09/05/17.
//

#include "Vocabulary.h"
#include <iostream>
#include <unordered_map>
#include <algorithm>

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

bool __terms_compare(const pair<string, size_t> &a, const pair<string, size_t> &b) {
    return b.second < a.second;
}

inline void PruneTerms(unordered_map<string, size_t> &terms, double threshold) {
    vector<pair<string, size_t>> entries;
    entries.resize(terms.size());

    size_t total = 0;
    size_t i = 0;
    for (auto entry = terms.begin(); entry != terms.end(); ++entry) {
        entries[i].first = entry->first;
        entries[i].second = entry->second;

        total += entry->second;

        i++;
    }

    std::sort(entries.begin(), entries.end(), __terms_compare);

    double counter = 0;
    size_t min_size = 0;
    for (auto entry = entries.begin(); entry != entries.end(); ++entry) {
        counter += entry->second;

        if (counter / total >= threshold) {
            min_size = entry->second;
            break;
        }
    }

    if (min_size > 1) {
        for (auto entry = terms.begin(); entry != terms.end(); /* no increment */) {
            if (entry->second < min_size)
                entry = terms.erase(entry);
            else
                ++entry;
        }
    }
}

const Vocabulary *Vocabulary::FromCorpus(CorpusReader &reader, double threshold) {
    // For model efficiency all source words have the lowest id possible
    unordered_map<string, size_t> src_terms;
    unordered_map<string, size_t> trg_terms;

    vector<string> src, trg;
    while (reader.Read(src, trg)) {
        for (auto w = src.begin(); w != src.end(); ++w)
            src_terms[*w] += 1;

        for (auto w = trg.begin(); w != trg.end(); ++w)
            trg_terms[*w] += 1;
    }

    if (threshold > 0) {
        PruneTerms(src_terms, threshold);
        PruneTerms(trg_terms, threshold);
    }
    
    for (auto term = src_terms.begin(); term != src_terms.end(); ++term)
        trg_terms.erase(term->first);

    Vocabulary *result = new Vocabulary();
    result->terms.reserve(src_terms.size() + trg_terms.size());

    for (auto term = src_terms.begin(); term != src_terms.end(); ++term)
        result->terms.push_back(term->first);
    for (auto term = trg_terms.begin(); term != trg_terms.end(); ++term)
        result->terms.push_back(term->first);

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