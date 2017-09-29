//
// Created by Davide  Caroselli on 29/09/16.
//

#ifndef SAPT_BILINGUALCORPUS_H
#define SAPT_BILINGUALCORPUS_H

#include <mmt/sentence.h>
#include <fstream>
#include <mmt/vocabulary/Vocabulary.h>

using namespace std;

namespace mmt {
    namespace sapt {

        class BilingualCorpus {
            friend class CorpusReader;

        public:
            BilingualCorpus(memory_t memory, const string &sourceFile, const string &targetFile,
                            const string &alignmentFile);

            const memory_t GetMemory() const {
                return memory;
            }

            static void
            List(const string &path, const string &sourceLang, const string &targetLang, vector<BilingualCorpus> &list);

        private:
            const memory_t memory;
            const string source;
            const string target;
            const string alignment;
        };

        class CorpusReader {
        public:
            CorpusReader(Vocabulary &vocabulary, const BilingualCorpus &corpus);

            bool Read(vector<wid_t> &outSource, vector<wid_t> &outTarget, alignment_t &outAlignment);

        private:
            Vocabulary &vb;
            bool drained;
            ifstream sourceStream;
            ifstream targetStream;
            ifstream alignmentStream;
        };

    }
}


#endif //SAPT_BILINGUALCORPUS_H
