//
// Created by Davide  Caroselli on 16/08/16.
//

#ifndef MMTCORE_INMEMORYVOCABULARY_H
#define MMTCORE_INMEMORYVOCABULARY_H

#include <string>
#include <boost/thread/shared_mutex.hpp>
#include <unordered_set>
#include <unordered_map>
#include <vector>
#include "Vocabulary.h"

using namespace std;

/**
 * In-Memory vocabulary, useful for bulk training with
 * no previous data.
 */
class InMemoryVocabulary : public Vocabulary {
public:
    InMemoryVocabulary(size_t estimatedSize = 10000000);

    virtual uint32_t Lookup(const string &word, bool putIfAbsent) override;

    virtual void
    Lookup(const vector<vector<string>> &buffer, vector<vector<uint32_t>> *output, bool putIfAbsent) override;

    void
    TEST_Lookup(unordered_map<string, uint32_t> &dict, bool putIfAbsent);

    virtual const bool ReverseLookup(uint32_t id, string *output) override;

    virtual const bool ReverseLookup(const vector<vector<uint32_t>> &buffer, vector<vector<string>> &output) override;

    void Flush(const string path);

private:
    unordered_map<string, uint32_t> dictionary;
    boost::shared_mutex lookupMutex;
    uint32_t id;
};


#endif //MMTCORE_INMEMORYVOCABULARY_H
