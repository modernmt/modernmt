//
// Created by Davide Caroselli on 27/07/16.
//

#include "VocabularyBuilder.h"
#include "Vocabulary.h"

VocabularyBuilder::VocabularyBuilder(size_t estimatedSize) {
    words.reserve(estimatedSize);
}

void VocabularyBuilder::Put(const string &word) {
    words.insert(word);
}

void VocabularyBuilder::Put(const unordered_set<string> &buffer) {
    words.insert(buffer.begin(), buffer.end());
}

void VocabularyBuilder::Flush(const string path) {
    Vocabulary vocabulary(path, true);

    uint32_t id = 0;

    for (auto it = words.begin(); it != words.end(); ++it) {
        vocabulary.Put(*it, kVocabularyWordIdStart + id);
        id++;
    }

    vocabulary.ForceCompaction();
    vocabulary.idGenerator.Reset(id);
}

size_t VocabularyBuilder::GetSize() {
    return words.size();
}
