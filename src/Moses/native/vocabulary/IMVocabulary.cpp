//
// Created by Davide  Caroselli on 16/08/16.
//

#include "IMVocabulary.h"
#include "Vocabulary.h"

IMVocabulary::IMVocabulary(size_t estimatedSize) : id(kVocabularyWordIdStart) {
    dictionary.reserve(estimatedSize);
}

uint32_t IMVocabulary::Put(const string &word) {
    pair<unordered_map<string, uint32_t>::iterator, bool> el = dictionary.emplace(word, id);

    if (el.second) { // element inserted
        return id++;
    } else {
        return el.first->second;
    }
}

vector<uint32_t> IMVocabulary::Put(const vector<string> &buffer) {
    vector<uint32_t> result;
    result.reserve(buffer.size());

    for (auto word = buffer.begin(); word != buffer.end(); ++word) {
        pair<unordered_map<string, uint32_t>::iterator, bool> el = dictionary.emplace(*word, id);

        if (el.second) { // element inserted
            result.push_back(id++);
        } else {
            result.push_back(el.first->second);
        }
    }

    return result;
}

void IMVocabulary::Put(const unordered_set<string> &buffer) {
    for (auto word = buffer.begin(); word != buffer.end(); ++word) {
        pair<unordered_map<string, uint32_t>::iterator, bool> el = dictionary.emplace(*word, id);

        if (el.second) { // element inserted
            id++;
        }
    }
}

size_t IMVocabulary::GetSize() {
    return dictionary.size();
}

void IMVocabulary::Flush(const string path) {
    Vocabulary vocabulary(path, true);

    for (auto it = dictionary.begin(); it != dictionary.end(); ++it) {
        vocabulary.Put(it->first, it->second);
    }

    vocabulary.ForceCompaction();
    vocabulary.idGenerator.Reset(id);
}
