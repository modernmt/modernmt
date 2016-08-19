//
// Created by Davide  Caroselli on 16/08/16.
//

#include "InMemoryVocabulary.h"
#include "PersistentVocabulary.h"

InMemoryVocabulary::InMemoryVocabulary(size_t estimatedSize) : id(kVocabularyWordIdStart) {
    dictionary.reserve(estimatedSize);
}

uint32_t InMemoryVocabulary::Lookup(const string &word, bool putIfAbsent) {
    boost::shared_lock<boost::shared_mutex> read_lock(lookupMutex);

    auto iterator = dictionary.find(word);
    if (iterator == dictionary.end()) {
        read_lock.unlock();

        if (putIfAbsent) {
            boost::upgrade_lock<boost::shared_mutex> lock(lookupMutex);
            boost::upgrade_to_unique_lock<boost::shared_mutex> uniqueLock(lock);

            pair<unordered_map<string, uint32_t>::iterator, bool> element = dictionary.emplace(word, id);

            if (element.second) { // element inserted
                return id++;
            } else {
                return element.first->second;
            }
        } else {
            return kVocabularyUnknownWord;
        }
    } else {
        return iterator->second;
    }
}

void
InMemoryVocabulary::Lookup(const vector<vector<string>> &buffer, vector<vector<uint32_t>> *output, bool putIfAbsent) {
    unordered_map<string, uint32_t> dict;
    dict.reserve(buffer.size() * 5);

    for (auto line = buffer.begin(); line != buffer.end(); ++line) {
        for (auto word = line->begin(); word != line->end(); ++word) {
            dict[*word] = kVocabularyUnknownWord;
        }
    }

    if (output) {
        output->reserve(buffer.size());

        for (auto line = buffer.begin(); line != buffer.end(); ++line) {
            vector<uint32_t> outLine;

            for (auto word = line->begin(); word != line->end(); ++word) {
                outLine.push_back(dict[*word]);
            }

            output->push_back(outLine);
        }
    }
}

void InMemoryVocabulary::TEST_Lookup(unordered_map<string, uint32_t> &dict, bool putIfAbsent) {
    for (auto iterator = dict.begin(); iterator != dict.end(); ++iterator) {
        iterator->second = Lookup(iterator->first, putIfAbsent);
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
    vocabulary.idGenerator.Reset(id);
}
