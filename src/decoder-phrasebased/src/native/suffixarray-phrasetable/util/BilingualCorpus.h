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
            BilingualCorpus(domain_t domain, const string &sourceFile, const string &targetFile,
                            const string &alignmentFile);

            const domain_t GetDomain() const {
                return domain;
            }

            static void
            List(const string &path, const string &sourceLang, const string &targetLang, vector<BilingualCorpus> &list);

        private:
            const domain_t domain;
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
