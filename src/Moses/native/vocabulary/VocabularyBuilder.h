//
// Created by Davide Caroselli on 27/07/16.
//

#ifndef MOSESDECODER_VOCABULARYBUILDER_H
#define MOSESDECODER_VOCABULARYBUILDER_H

#include <string>
#include <unordered_set>
#include <mutex>

using namespace std;

class VocabularyBuilder {
public:
    VocabularyBuilder(size_t estimatedSize = 10000000);

    void Put(const string &word);

    void Put(const unordered_set<string> &buffer);

    void Flush(const string path);

    size_t GetSize();

private:
    unordered_set<string> words;
};


#endif //MOSESDECODER_VOCABULARYBUILDER_H
