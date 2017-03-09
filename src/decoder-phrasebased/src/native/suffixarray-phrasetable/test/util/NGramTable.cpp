//
// Created by Davide Caroselli on 02/10/16.
//

#include "NGramTable.h"

using namespace mmt;
using namespace mmt::sapt;
using namespace mmt::sapt::test;

static vector<wid_t> SubVector(const vector<wid_t> &sentence, size_t offset, size_t length) {
    vector<wid_t> output(length);

    for (size_t i = 0; i < length; ++i)
        output[i] = sentence[offset + i];

    return output;
}

NGramTable::NGramTable(uint8_t order) : order(order) {
    ngrams.resize((size_t) order);
}

void NGramTable::Load(const BilingualCorpus &corpus, bool loadSource) {
    vector<wid_t> source, target;
    alignment_t alignment;

    CorpusReader reader(corpus);
    while (reader.Read(source, target, alignment)) {
        vector<wid_t> &sentence = loadSource ? source : target;

        for (size_t iword = 0; iword < sentence.size(); ++iword) {
            for (size_t iorder = 0; iorder < order; ++iorder) {
                if (iword + iorder >= sentence.size())
                    break;

                vector<wid_t> ngram = SubVector(sentence, iword, iorder + 1);
                ngrams[iorder][ngram]++;
            }
        }
    }
}
