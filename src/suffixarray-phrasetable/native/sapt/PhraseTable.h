//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_PHRASETABLE_H
#define SAPT_PHRASETABLE_H

#include <iostream>
#include <sstream>
#include <map>

#include <mmt/IncrementalModel.h>
#include <mmt/aligner/Aligner.h>
#include "Options.h"

using namespace std;

namespace mmt {
    namespace sapt {


        struct TranslationOption {
            vector<wid_t> targetPhrase;
            alignment_t alignment;
            vector<float> scores;

            TranslationOption(size_t scoreSize) {
                scores.resize(scoreSize);
            }
            void SetScores(std::vector<float> &sc){
                scores.assign(sc.begin(),sc.end());
            }
            void SetTargetPhrase(vector<wid_t> &tp){
                targetPhrase.assign(tp.begin(),tp.end());
            }

            string ToString() const {
                ostringstream repr;
                for (auto w = targetPhrase.begin(); w != targetPhrase.end(); ++w)
                    repr << *w << " ";
                repr << " |||";
                for (auto a = alignment.begin(); a != alignment.end(); ++a)
                    repr << " " << a->first << "-" << a->second;
                repr << " |||";
                for (auto o = scores.begin(); o != scores.end(); ++o)
                    repr << " " << *o;

                return repr.str();
            }

//friend
            friend std::ostream &operator<<(std::ostream &os, const TranslationOption &option)
            {
                os << option.ToString();
                return os;
            }

            bool operator<(const TranslationOption& rhs) const {
                //TODO: in the original UG sapt two options are equal even if their alignments differ, and (probably) the winning alignment is the more frequent; no idea what count is associated with the winning option
                //TODO: With this comparison function two options are equal if and only both targetPhrases and alignments are the same
                //TODO: To replicate the original UG SAPT behavior, commentout the portion of code related to the alignment
                //check whether the 2 targetPhrases have the same length
                if (this->targetPhrase.size() < rhs.targetPhrase.size()) return true;
                if (this->targetPhrase.size() > rhs.targetPhrase.size()) return false;

                /* comment to replicate original UG SAPT
                //check whether the 2 alignments have the same length
                if (this->alignment.size() < rhs.alignment.size()) return true;
                if (this->alignment.size() > rhs.alignment.size()) return false;
                */

                //check whether the all words of the 2 targetPhrases are the same
                for (size_t i = 0; i < this->targetPhrase.size(); ++i) {
                    if (this->targetPhrase[i] < rhs.targetPhrase[i]) return true;
                    if (this->targetPhrase[i] > rhs.targetPhrase[i]) return false;
                }

                /* comment to replicate original UG SAPT
                //check whether the all words of the 2 alignments are the same
                for (size_t i = 0; i < this->alignment.size(); ++i) {
                    if (this->alignment[i].first < rhs.alignment[i].first) return true;
                    if (this->alignment[i].first > rhs.alignment[i].first) return false;
                    if (this->alignment[i].second < rhs.alignment[i].second) return true;
                    if (this->alignment[i].second > rhs.alignment[i].second) return false;
                }
                */
                return false;
            }

        };

        class PhraseTable : public IncrementalModel {
            typedef std::map<TranslationOption, size_t> OptionsMap_t;
            typedef std::vector<TranslationOption> OptionsVec_t;

        public:
            PhraseTable(const string &modelPath, const Options &options = Options(), mmt::Aligner *alignerModel=NULL);

            ~PhraseTable();

            void NormalizeContext(context_t *context);

            void GetTargetPhraseCollection(const vector<wid_t> &phrase, size_t limit, vector<TranslationOption> &outOptions, context_t *context);

            // TODO: just for testing purpose, must be removed asap
            void *__GetSuffixArray();

            /* IncrementalModel */

            virtual void Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                             const std::vector<wid_t> &target, const alignment_t &alignment) override;

            virtual vector<updateid_t> GetLatestUpdatesIdentifier() override;

            bool isDebug() const {
                return debug;
            }

            void setDebug(bool debug) {
                PhraseTable::debug = debug;
            }

        private:
            struct pt_private;
            pt_private *self;
            size_t numScoreComponent;
            bool debug;

            void GetTranslationOptions(const vector<wid_t> &sourcePhrase,
                                       const std::vector<wid_t> &sourceSentence,
                                       const std::vector<wid_t> &targetSentence,
                                       const alignment_t &alignment,
                                       const std::vector<length_t> &offsets,
                                       OptionsMap_t &optionsMap);

            void ExtractPhrasePairs(const std::vector<wid_t> &sourceSentence,
                                    const std::vector<wid_t> &targetSentence,
                                    const alignment_t &alignment,
                                    const std::vector<bool> &targetAligned,
                                    int sourceStart, int sourceEnd, int targetStart, int targetEnd,
                                    OptionsMap_t &optionsMap);

            void ScoreTranslationOptions(OptionsMap_t &optionsMap, const vector<wid_t> &sourcePhrase, size_t NumberOfSamples);

            void GetLexicalScores(const vector<wid_t> &sourcePhrase, const TranslationOption &option, float &fwdScore, float &bwdScore);
/*
            float GetForwardLexicalScore(length_t sourceWord, length_t targetWord);
            float GetBackwardLexicalScore(length_t sourceWord, length_t targetWord);
*/
        };
    }
}


#endif //SAPT_PHRASETABLE_H
