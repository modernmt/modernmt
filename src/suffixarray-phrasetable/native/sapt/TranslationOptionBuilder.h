//
// Created by Davide  Caroselli on 19/10/16.
//

#ifndef SAPT_TRANSLATIONOPTIONBUILDER_H
#define SAPT_TRANSLATIONOPTIONBUILDER_H

#include <mmt/sentence.h>
#include <suffixarray/sample.h>

using namespace std;

namespace mmt {
    namespace sapt {

        class TranslationOptionBuilder {
        public:

            static void Extract(const vector<wid_t> &sourcePhrase, const vector<sample_t> &samples,
                                vector<TranslationOptionBuilder> &output);

            TranslationOptionBuilder(const vector<wid_t> &phrase);

            const alignment_t &GetBestAlignment() const;

            inline const vector<wid_t> &GetPhrase() const {
                return phrase;
            }

            size_t GetCount() const {
                return count;
            }

        private:
            unordered_map<alignment_t, size_t, alignment_hash> alignments;
            vector<wid_t> phrase;
            size_t count;

            void Add(const alignment_t &alignment);
        };

    }
}


#endif //SAPT_TRANSLATIONOPTIONBUILDER_H
