//
// Created by Davide Caroselli on 02/10/16.
//

#ifndef SAPT_NGRAMTABLE_H
#define SAPT_NGRAMTABLE_H

#include <util/BilingualCorpus.h>
#include <util/hashutils.h>

using namespace std;

namespace mmt {
    namespace sapt {
        namespace test {

            struct phrase_hash {
                size_t operator()(const vector<wid_t> &x) const {
                    return (size_t) (words_hash(x));
                }
            };

            class NGramTable {
            public:
                NGramTable(uint8_t order);

                void Load(const BilingualCorpus &corpus, bool loadSource = true);

                const unordered_map<vector<wid_t>, size_t, phrase_hash> &GetNGrams(uint8_t order) const {
                    if (order == 0 || order > ngrams.size())
                        throw invalid_argument("Invalid order " + to_string((int) order));
                    return ngrams[order - 1];
                };

            private:
                const uint8_t order;
                vector<unordered_map<vector<wid_t>, size_t, phrase_hash>> ngrams;
            };

        }
    }
}


#endif //SAPT_NGRAMTABLE_H
