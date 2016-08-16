//
// Created by Davide  Caroselli on 16/08/16.
//

#ifndef MOSESDECODER_IMVOCABULARY_H
#define MOSESDECODER_IMVOCABULARY_H

#include <string>
#include <unordered_set>
#include <unordered_map>
#include <vector>

using namespace std;

/**
 * In-Memory vocabulary, useful for bulk training with
 * no previous data.
 */
class IMVocabulary {
public:
    IMVocabulary(size_t estimatedSize = 10000000);

    uint32_t Put(const string &word);

    vector<uint32_t> Put(const vector<string> &buffer);

    void Put(const unordered_set<string> &buffer);

    void Flush(const string path);

    size_t GetSize();

private:
    unordered_map<string, uint32_t> dictionary;
    uint32_t id;
};


#endif //MOSESDECODER_IMVOCABULARY_H
