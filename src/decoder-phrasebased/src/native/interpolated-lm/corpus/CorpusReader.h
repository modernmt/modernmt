//
// Created by Davide  Caroselli on 08/09/16.
//

#ifndef ILM_CORPUSREADER_H
#define ILM_CORPUSREADER_H

#include <istream>
#include <lm/LM.h>
#include <memory>
#include <sstream>

using namespace std;

namespace mmt {
    namespace ilm {

        class CorpusReader {
        public:
            CorpusReader(const string &corpus);

            CorpusReader(istream *stream);

            bool Read(vector <wid_t> &outSentence);

            static inline void ParseLine(const string &line, vector <wid_t> &output) {
                output.clear();

                std::stringstream stream(line);
                wid_t word;

                while (stream >> word)
                    output.push_back(word);
            }

        private:
            bool drained;
            shared_ptr <istream> input;
        };

    }
}

#endif //ILM_CORPUSREADER_H
