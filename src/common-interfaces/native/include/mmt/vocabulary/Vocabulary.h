//
// Created by Davide  Caroselli on 02/09/16.
//

#ifndef MMT_COMMON_INTERFACES_VOCABULARY_H
#define MMT_COMMON_INTERFACES_VOCABULARY_H

#include <mmt/sentence.h>
#include <string>
#include <vector>

using namespace std;

namespace mmt {

    const wid_t kVocabularyUnknownWord = 0;
    const wid_t kVocabularyWordIdStart = 1000;

    class Vocabulary {
    public:
        virtual wid_t Lookup(const string &word, bool putIfAbsent) = 0;

        virtual void
        Lookup(const vector<vector<string>> &buffer, vector<vector<wid_t>> *output, bool putIfAbsent) = 0;

        virtual const bool ReverseLookup(wid_t id, string *output) = 0;

        virtual const bool ReverseLookup(const vector<vector<wid_t>> &buffer, vector<vector<string>> &output) = 0;

        virtual ~Vocabulary() {};
    };
}


#endif //MMT_COMMON_INTERFACES_VOCABULARY_H
