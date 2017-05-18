//
// Created by Davide  Caroselli on 08/09/16.
//

#ifndef ILM_CORPUSREADER_H
#define ILM_CORPUSREADER_H

#include <istream>
#include <lm/LM.h>
#include <memory>
#include <sstream>
#include <mmt/vocabulary/Vocabulary.h>

using namespace std;

namespace mmt {
    namespace ilm {

        class CorpusReader {
        public:
            CorpusReader(Vocabulary &vocabulary, const string &filename);

            CorpusReader(Vocabulary &vocabulary, istream *stream);

            bool Read(vector<wid_t> &outSentence);

        private:
            bool drained;
            shared_ptr<istream> input;
            Vocabulary &vb;
        };

    }
}

#endif //ILM_CORPUSREADER_H
