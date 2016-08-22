//
// Created by Davide  Caroselli on 16/08/16.
//

#include "InMemoryVocabulary.h"
#include "PersistentVocabulary.h"

InMemoryVocabulary::InMemoryVocabulary(size_t estimatedSize) : id(kVocabularyWordIdStart) {
    dictionary.reserve(estimatedSize);
}

uint32_t InMemoryVocabulary::Lookup(const string &word, bool putIfAbsent) {
    pair<unordered_map<string, uint32_t>::iterator, bool> el = dictionary.emplace(word, id);

    if (el.second) { // element inserted
        return id++;
    } else {
        return el.first->second;
    }
}

void
InMemoryVocabulary::Lookup(const vector<vector<string>> &buffer, vector<vector<uint32_t>> *output, bool putIfAbsent) {
    if (output)
        output->reserve(buffer.size());

    for (auto line = buffer.begin(); line != buffer.end(); ++line) {
        vector<uint32_t> outLine;

        for (auto word = line->begin(); word != line->end(); ++word) {
            pair<unordered_map<string, uint32_t>::iterator, bool> el = dictionary.emplace(*word, id);
            uint32_t newid = el.second ? id++ : el.first->second;

            if (output)
                outLine.push_back(newid);
        }

        if (output)
            output->push_back(outLine);
    }
}

const bool InMemoryVocabulary::ReverseLookup(uint32_t id, string *output) {
    return false; // Not supported
}

const bool InMemoryVocabulary::ReverseLookup(const vector<vector<uint32_t>> &buffer, vector<vector<string>> &output) {
    return false; // Not supported
}

void InMemoryVocabulary::Flush(const string path) {
    PersistentVocabulary vocabulary(path, true);

    for (auto it = dictionary.begin(); it != dictionary.end(); ++it) {
        vocabulary.Put(it->first, it->second);
    }

    vocabulary.ForceCompaction();
    vocabulary.ResetId(id);
}
