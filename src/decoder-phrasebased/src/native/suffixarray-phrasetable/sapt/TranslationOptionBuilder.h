//
// Created by Davide  Caroselli on 19/10/16.
//

#ifndef SAPT_TRANSLATIONOPTIONBUILDER_H
#define SAPT_TRANSLATIONOPTIONBUILDER_H

#include <mmt/sentence.h>
#include <suffixarray/sample.h>
#include "TranslationOption.h"

using namespace std;

namespace mmt {
    namespace sapt {

        class TranslationOptionBuilder;

        typedef unordered_map<vector<wid_t>, TranslationOptionBuilder, phrase_hash> optionsmap_t;

        class TranslationOptionBuilder {

        public:
            static void Extract(const vector<wid_t> &sourcePhrase, const vector<sample_t> &samples,
                                vector<TranslationOptionBuilder> &output, size_t &validSamples);

            TranslationOptionBuilder(const vector<wid_t> &phrase);

            const alignment_t &GetBestAlignment() const;

            inline const vector<wid_t> &GetPhrase() const {
                return phrase;
            }

            size_t GetCount() const {
                return count;
            }

            const TranslationOption::Orientations &GetOrientations() const {
                return orientations;
            }

            void Add(const alignment_t &alignment);

        private:
            unordered_map<alignment_t, size_t, alignment_hash> alignments;
            vector<wid_t> phrase;
            size_t count;
            TranslationOption::Orientations orientations;


            static void Extract(const vector<wid_t> &sourcePhrase, const sample_t &sample, int offset,
                                vector<bool> &targetAligned, optionsmap_t &map, size_t &validSamples);

            static void ExtractOptions(const vector<wid_t> &sourceSentence, const vector<wid_t> &targetSentence,
                                       const alignment_t &allAlignment, const alignment_t &inBoundAlignment,
                                       const vector<bool> &targetAligned,
                                       int sourceStart, int sourceEnd, int targetStart, int targetEnd,
                                       optionsmap_t &map, bool &isValid);
        };

    }
}


#endif //SAPT_TRANSLATIONOPTIONBUILDER_H
