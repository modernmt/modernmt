//
// Created by Davide  Caroselli on 27/09/16.
//

#include <algorithm>
#include <boost/math/distributions/binomial.hpp>
#include <suffixarray/SuffixArray.h>
#include <util/hashutils.h>

#include "PhraseTable.h"
#include "UpdateManager.h"
#include "TranslationOptionBuilder.h"

using namespace mmt;
using namespace mmt::sapt;


typedef vector<bool> bitvector;

struct PhraseTable::pt_private {
    SuffixArray *index;
    UpdateManager *updates;
    Aligner *aligner;

    size_t numberOfSamples;
};

PhraseTable::PhraseTable(const string &modelPath, const Options &options, Aligner *aligner) {
    self = new pt_private();
    self->index = new SuffixArray(modelPath, options.prefix_length);
    self->updates = new UpdateManager(self->index, options.update_buffer_size, options.update_max_delay);
    self->aligner = aligner;
    self->numberOfSamples = options.samples;
}

PhraseTable::~PhraseTable() {
    delete self->updates;
    delete self->index;
    delete self;
}

void PhraseTable::Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                      const std::vector<wid_t> &target, const alignment_t &alignment) {
    self->updates->Add(id, domain, source, target, alignment);
}

unordered_map<stream_t, seqid_t> PhraseTable::GetLatestUpdatesIdentifier() {
    const vector<seqid_t> &streams = self->index->GetStreams();

    unordered_map<stream_t, seqid_t> result;
    result.reserve(streams.size());

    for (size_t i = 0; i < streams.size(); ++i) {
        if (streams[i] >= 0)
            result[(stream_t) i] = streams[i];
    }

    return result;
}

/* Translation Options extraction */

//typedef unordered_map<TranslationOption, size_t, TranslationOption::hash> optionsmap_t;
/*
static void ExtractPhrasePairs(const vector<wid_t> &sourceSentence, const vector<wid_t> &targetSentence,
                               const alignment_t &allAlignment, const alignment_t &inBoundAlignment, const vector<bool> &targetAligned,
                               int sourceStart, int sourceEnd, int targetStart, int targetEnd,
                               optionsmap_t &outOptions, bool &isValid) {

    isValid = false;

    if (targetEnd < 0) // 0-based indexing.
        return;

    // Check if alignment points are consistent. if yes, copy
    alignment_t currentAlignments;
    for (auto alignPoint = inBoundAlignment.begin(); alignPoint != inBoundAlignment.end(); ++alignPoint) {

        //checking whether there are other alignment points outside the current phrase pair; if yes, return doing nothing, because the phrase pair is not valid
        if (((alignPoint->first >= sourceStart) && (alignPoint->first <= sourceEnd)) &&
            ((alignPoint->second < targetStart) || (alignPoint->second > targetEnd))) {
            return;
        }
        if (((alignPoint->second >= targetStart) && (alignPoint->second <= targetEnd)) &&
            ((alignPoint->first < sourceStart) || (alignPoint->first > sourceEnd))) {
            return;
        }

        currentAlignments.push_back(*alignPoint);
    }


    int ts = targetStart;
    while (true) {
        int te = targetEnd;
        while (true) {
            TranslationOption option;
            option.targetPhrase.insert(option.targetPhrase.begin(),
                                       targetSentence.begin() + ts,
                                       targetSentence.begin() + te + 1);

            alignment_t tmp_alignment = currentAlignments;
            //re-set the word positions within the phrase pair, regardless the sentence context
            for (auto a = tmp_alignment.begin(); a != tmp_alignment.end(); ++a) {
                a->first -= sourceStart;
                a->second -= ts;
            }
*/
/*
            //compute orientation for this phrase pair
            ReorderingType forwardOrientation = kTOMonotonicOrientation; //dummy computation
            ReorderingType backwardOrientation = kTOSwapOrientation; //dummy computation
            */
/*



            //determine fwd and bwd phrase orientation
            size_t slen1; // length of source sentence in case of forward
            size_t slen2; // length of target sentence in case of forward
            bool flip=0;
            if (flip){
                slen1 = targetSentence.size();
                slen2 = sourceSentence.size();
            } else {
                slen1 = sourceSentence.size();
                slen2 = targetSentence.size();
            }

            size_t start = sourceStart;
            size_t stop = sourceEnd+1;

            std::vector<std::vector<ushort> > aln1(slen1); //long as the source sentence (or target if flipped)
            std::vector<std::vector<ushort> > aln2(slen2); //long as the target sentence (or source if flipped)
            bitvector forbidden(slen2);

            size_t src,trg;
            size_t lft = forbidden.size();
            size_t rgt = 0;

            for ( auto align = allAlignment.begin(); align != allAlignment.end(); ++ align){

                if (flip) {
                    src = align->second;
                    trg = align->first;
                } else {
                    src=align->first;
                    trg=align->second;
                }

                assert(src < slen1);
                assert(trg < slen2);

                if (src < start || src >= stop) {
                    forbidden.at(trg) = true;
                } else {
                    lft = std::min(lft,trg);
                    rgt = std::max(rgt,trg);
                }
                aln1[src].push_back(trg);
                aln2[trg].push_back(src);
            }

            bool computeOrientation = true;

            if (lft > rgt) {
                computeOrientation = false;
            } else {
                for (size_t i = lft; i <= rgt; ++i) {
                    if (forbidden[i]) {
                        computeOrientation = false;
                    }
                }
            }

            size_t s1, s2 = lft;
            for (s1 = s2; s1 && !forbidden[s1-1]; --s1) {};
            size_t e1 = rgt+1, e2;
            for (e2 = e1; e2 < forbidden.size() && !forbidden[e2]; ++e2) {};

            auto ptr = outOptions.find(option);
            if (ptr != outOptions.end()) { //this option is already present, update the alignments
                ((TranslationOption&) ptr->first).InsertAlignment(tmp_alignment);
                if (computeOrientation) {
                    ((TranslationOption &) ptr->first).UpdateOrientation(find_po_fwd(aln1, aln2, start, stop, s1, e2),
                                                                         find_po_bwd(aln1,aln2,start,stop,s1,e2));
                }
                ptr->second = ptr->second + 1;
            } else {
                //insert the (possibly new) alignment into the options
                option.InsertAlignment(tmp_alignment);
                if (computeOrientation) {
                    option.UpdateOrientation(find_po_fwd(aln1, aln2, start, stop, s1, e2), find_po_bwd(aln1,aln2,start,stop,s1,e2));
                }
                outOptions[option] = 1;
            }
            isValid = true;

            te += 1;
            // if fe is in word alignment or out-of-bounds
            if (te == targetSentence.size() || targetAligned[te]) {
                break;
            }
        }

        ts -= 1;
        // if fs is in word alignment or out-of-bounds
        if (ts < 0 || targetAligned[ts]) {
            break;
        }
    }
}
*/
/*

static void GetTranslationOptionsFromSample(const vector<wid_t> &sourcePhrase, const sample_t &sample, optionsmap_t &outOptions, size_t &validSample) {
    // keeps a vector to know whether a target word is aligned.
    vector<bool> targetAligned(sample.target.size(), false);
    for (auto alignPoint = sample.alignment.begin(); alignPoint != sample.alignment.end(); ++alignPoint)
        targetAligned[alignPoint->second] = true;

    // for each occurrence of the source in the sampled sentence pair
    for (auto offset = sample.offsets.begin(); offset != sample.offsets.end(); ++offset) {
        // get source position lowerBound  and  upperBound
        int sourceStart = *offset; // lowerBound is always larger than or equal to 0
        int sourceEnd = sourceStart + sourcePhrase.size() - 1; // upperBound is always larger than or equal to 0, because sourcePhrase.size()>=1

        // find the minimally matching foreign phrase
        int targetStart = sample.target.size() - 1;
        int targetEnd = -1;

        for (auto alignPoint = sample.alignment.begin(); alignPoint != sample.alignment.end(); ++alignPoint) {
            if ((alignPoint->first >= sourceStart) && (alignPoint->first <= sourceEnd)) {
                targetStart = min((int) alignPoint->second, targetStart);
                targetEnd = max((int) alignPoint->second, targetEnd);
            }
        }

        alignment_t inBoundsAlignment;
        for (auto alignPoint = sample.alignment.begin(); alignPoint != sample.alignment.end(); ++alignPoint) {
            if (((alignPoint->first >= sourceStart) && (alignPoint->first <= sourceEnd)) ||
                ((alignPoint->second >= targetStart) && (alignPoint->second <= targetEnd))) {
                inBoundsAlignment.push_back(*alignPoint);
            }
        }

        bool isValid = false;
        ExtractPhrasePairs(sample.source, sample.target, sample.alignment, inBoundsAlignment, targetAligned,
                           sourceStart, sourceEnd, targetStart, targetEnd, outOptions, isValid);
        if (isValid) ++validSample;
    }
}
*/

/* Translation Options scoring */

static void GetLexicalScores(Aligner *aligner, const vector<wid_t> &phrase, const TranslationOption &option,
                             float &fwdScore, float &bwdScore) {
    vector<vector<float>> fwdWordProb(option.targetPhrase.size());
    vector<vector<float>> bwdWordProb(phrase.size());
    size_t sSize = phrase.size();
    size_t tSize = option.targetPhrase.size();

    //computes the lexical probabilities on the best alignment only
    for (auto a = option.alignment.begin(); a != option.alignment.end(); ++a) {
        wid_t sWord = phrase[a->first];
        wid_t tWord = option.targetPhrase[a->second];
        fwdWordProb[a->second].push_back(aligner->GetForwardProbability(sWord, tWord));  //P(tWord | sWord)
        bwdWordProb[a->first].push_back(aligner->GetBackwardProbability(sWord, tWord));  //P(sWord | tWord)

    }
    fwdScore = 0.0;
    for (size_t ti = 0; ti < tSize; ++ti) {
        float tmpProb = 0.0;
        size_t tmpSize = fwdWordProb[ti].size();

        if (tmpSize > 0) {
            for (size_t i = 0; i < tmpSize; ++i) {
                tmpProb += fwdWordProb[ti][i];
            }
            tmpProb /= tmpSize;
        } else {
            tmpProb = aligner->GetTargetNullProbability(option.targetPhrase[ti]);
        }
        //should never happen that tmpProb <= 0
        fwdScore += (tmpProb <= 0.0) ? -9 : log(tmpProb);
    }

    bwdScore = 0.0;
    for (size_t si = 0; si < sSize; ++si) {
        float tmpProb = 0.0;
        size_t tmpSize = bwdWordProb[si].size();

        if (tmpSize > 0) {
            for (size_t i = 0; i < tmpSize; ++i) {
                tmpProb += bwdWordProb[si][i];
            }
            tmpProb /= tmpSize;
        } else {
            tmpProb = aligner->GetSourceNullProbability(phrase[si]);
        }

        //should never happen that tmpProb <= 0
        bwdScore += (tmpProb <= 0.0) ? -9 : log(tmpProb);
    }
}

static float lbop(float succ, float tries, float confidence) {
    if (confidence == 0)
        return succ / tries;
    else
        return (float) boost::math::binomial_distribution<>::find_lower_bound_on_p(tries, succ, confidence);
}



static void MakeTranslationOptions(SuffixArray *index, Aligner *aligner,
                                   const vector<wid_t> &phrase, const vector<sample_t> &samples,
                                   vector<TranslationOption> &output) {

    size_t validSamples = 0;
    vector<TranslationOptionBuilder> builders;
    TranslationOptionBuilder::Extract(phrase, samples, builders, validSamples);

    static constexpr float confidence = 0.01;

    /*
    size_t sampleSourceFrequency = 0;
    for (auto entry = builders.begin(); entry != builders.end(); ++entry) {
        sampleSourceFrequency += entry->GetCount();
    }
    */

    //compute frequency-based and (possibly) lexical-based scores for all options
    //create the actual Translation option objects, setting the "best" alignment,
    size_t SampleSourceFrequency = validSamples;
    size_t GlobalSourceFrequency = index->CountOccurrences(true, phrase);

    for (auto entry = builders.begin(); entry != builders.end(); ++entry) {
        size_t GlobalTargetFrequency = index->CountOccurrences(false, entry->GetPhrase());

        float fwdScore = log(lbop(entry->GetCount(),
                                  std::max(entry->GetCount(), SampleSourceFrequency),
                                  confidence));
        float bwdScore = log(lbop(entry->GetCount(),
                                  std::max(entry->GetCount(), (size_t) round((float) SampleSourceFrequency * GlobalTargetFrequency / GlobalSourceFrequency)),
                                  confidence));

        float fwdLexScore = 0.f;
        float bwdLexScore = 0.f;

        TranslationOption option;
        option.alignment = entry->GetBestAlignment();
        option.targetPhrase = entry->GetPhrase();
        option.orientationCounts = entry->GetOrientationCounts();

        if (aligner)
            GetLexicalScores(aligner, phrase, option, fwdLexScore, bwdLexScore);

        option.scores[kTOForwardProbability] = fwdScore;
        option.scores[kTOBackwardProbability] = min(0.f, bwdScore);
        option.scores[kTOForwardLexicalProbability] = fwdLexScore;
        option.scores[kTOBackwardLexicalProbability] = bwdLexScore;

        output.push_back(option);
    }
}

/* SAPT methods */
/*
static void MakeOptions(SuffixArray *index, Aligner *aligner,
                        const vector<wid_t> &phrase, const vector<sample_t> &samples,
                        vector<TranslationOption> &output) {
    optionsmap_t options;

    size_t validSample = 0;
    for (auto sample = samples.begin(); sample != samples.end(); ++sample)
        GetTranslationOptionsFromSample(phrase, *sample, options, validSample);

    ScoreTranslationOptions(index, aligner, phrase, options, validSample);

    for (auto entry = options.begin(); entry != options.end(); ++entry)
        output.push_back(entry->first);
}
*/

vector<TranslationOption> PhraseTable::GetTranslationOptions(const vector<wid_t> &phrase, context_t *context) {
    vector<sample_t> samples;
    self->index->GetRandomSamples(phrase, self->numberOfSamples, samples, context);

    vector<TranslationOption> result;
    MakeTranslationOptions(self->index, self->aligner, phrase, samples, result);

    return result;
}



void PhraseTable::GetSamples(const vector<wid_t> &phrase, vector<vector<wid_t> > &sourceSentences, vector<vector<wid_t> > &targetSentences, vector<alignment_t > &alignments, context_t *context){
    vector<sample_t> samples;
    self->index->GetRandomSamples(phrase, self->numberOfSamples, samples, context);
    for (auto sample = samples.begin(); sample != samples.end(); ++sample){
        sourceSentences.push_back(sample->source);
        targetSentences.push_back(sample->target);
        alignments.push_back(sample->alignment);
    }
}


translation_table_t PhraseTable::GetAllTranslationOptions(const vector<wid_t> &sentence, context_t *context) {
    translation_table_t ttable;

    for (size_t start = 0; start < sentence.size(); ++start) {
        Collector *collector = self->index->NewCollector(context);

        vector<wid_t> phrase;
        vector<wid_t> phraseDelta;

        for (size_t end = start; end < sentence.size(); ++end) {
            wid_t word = sentence[end];
            phrase.push_back(word);
            phraseDelta.push_back(word);

            if (ttable.find(phrase) == ttable.end()) {
                vector<sample_t> samples;
                collector->Extend(phraseDelta, self->numberOfSamples, samples);
                phraseDelta.clear();

                if (samples.empty())
                    break;

                vector<TranslationOption> options;
                MakeTranslationOptions(self->index, self->aligner, phrase, samples, options);

                ttable[phrase] = options;
            }
        }
       delete collector;
    }

    return ttable;
}
