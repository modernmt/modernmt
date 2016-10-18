//
// Created by Davide  Caroselli on 27/09/16.
//

#include <algorithm>
#include <boost/math/distributions/binomial.hpp>
#include <suffixarray/SuffixArray.h>

#include "PhraseTable.h"
#include "UpdateManager.h"

using namespace mmt;
using namespace mmt::sapt;

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

vector<updateid_t> PhraseTable::GetLatestUpdatesIdentifier() {
    const vector<seqid_t> &streams = self->index->GetStreams();

    vector<updateid_t> result;
    result.reserve(streams.size());

    for (size_t i = 0; i < streams.size(); ++i) {
        if (streams[i] != 0)
            result.push_back(updateid_t((stream_t) i, streams[i]));

    }

    return result;
}

/* Translation Options extraction */

typedef unordered_map<TranslationOption, size_t, TranslationOption::hash> optionsmap_t;

static void ExtractPhrasePairs(const vector<wid_t> &sourceSentence, const vector<wid_t> &targetSentence,
                               const alignment_t &alignment, const vector<bool> &targetAligned,
                               int sourceStart, int sourceEnd, int targetStart, int targetEnd,
                               optionsmap_t &outOptions) {
    if (targetEnd < 0) // 0-based indexing.
        return;

    // Check if alignment points are consistent. if yes, copy
    alignment_t currentAlignments;
    for (auto alignPoint = alignment.begin(); alignPoint != alignment.end(); ++alignPoint) {

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

            auto ptr = outOptions.find(option);
            if (ptr != outOptions.end()) { //this option is already present, update the alignments
                ((TranslationOption*) &ptr->first)->InsertAlignment(tmp_alignment);
                ptr->second = ptr->second + 1;
            } else {
                //insert the (possibly new) alignment into the options
                option.InsertAlignment(tmp_alignment);

                outOptions[option] = 1;
            }
            ptr = outOptions.find(option);

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

static void GetTranslationOptionsFromSample(const vector<wid_t> &sourcePhrase, const sample_t &sample,
                                            optionsmap_t &outOptions) {
    // keeps a vector to know whether a target word is aligned.
    vector<bool> targetAligned(sample.target.size(), false);
    for (auto alignPoint = sample.alignment.begin(); alignPoint != sample.alignment.end(); ++alignPoint)
        targetAligned[alignPoint->second] = true;

    // for each occurrence of the source in the sampled sentence pair
    for (auto offset = sample.offsets.begin(); offset != sample.offsets.end(); ++offset) {
        // get source position lowerBound  and  upperBound
        int sourceStart = *offset; // lowerBound is always larger than or equal to 0
        int sourceEnd = sourceStart + sourcePhrase.size() -
                        1; // upperBound is always larger than or equal to 0, because sourcePhrase.size()>=1

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

        ExtractPhrasePairs(sample.source, sample.target, inBoundsAlignment, targetAligned,
                           sourceStart, sourceEnd, targetStart, targetEnd, outOptions);
    }
}

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
    if(confidence == 0)
        return succ / tries;
    else
        return (float) boost::math::binomial_distribution<>::find_lower_bound_on_p(tries, succ, confidence);
}

static void ScoreTranslationOptions(SuffixArray *index, Aligner *aligner,
                                    const vector<wid_t> &phrase, optionsmap_t &options) {

    static constexpr float confidence = 0.01;

    size_t SampleSourceFrequency = 0;
    for (auto entry = options.begin(); entry != options.end(); ++entry) {
        //set the best alignment for each option
        ((TranslationOption) entry->first).SetBestAlignment();
        SampleSourceFrequency += entry->second;
    }

    size_t GlobalSourceFrequency = index->CountOccurrences(true, phrase);

    for (auto entry = options.begin(); entry != options.end(); ++entry) {
        size_t GlobalTargetFrequency = index->CountOccurrences(false, entry->first.targetPhrase);

        float fwdScore = log(lbop(entry->second, SampleSourceFrequency, confidence));
        float bwdScore = log(lbop(entry->second, std::max((float) entry->second, (float) SampleSourceFrequency *
                             GlobalTargetFrequency / GlobalSourceFrequency), confidence));
        float fwdLexScore = 0.f;
        float bwdLexScore = 0.f;

        if (aligner)
            GetLexicalScores(aligner, phrase, entry->first, fwdLexScore, bwdLexScore);

        vector<float> &scores = (vector<float> &) entry->first.scores;
        scores[kTOForwardProbability] = fwdScore;
        scores[kTOBackwardProbability] = min(0.f, bwdScore);
        scores[kTOForwardLexicalProbability] = fwdLexScore;
        scores[kTOBackwardLexicalProbability] = bwdLexScore;
    }
}

/* SAPT methods */

static void MakeOptions(SuffixArray *index, Aligner *aligner,
                        const vector<wid_t> &phrase, const vector<sample_t> &samples,
                        vector<TranslationOption> &output) {
    optionsmap_t options;

    for (auto sample = samples.begin(); sample != samples.end(); ++sample)
        GetTranslationOptionsFromSample(phrase, *sample, options);

    ScoreTranslationOptions(index, aligner, phrase, options);

    for (auto entry = options.begin(); entry != options.end(); ++entry)
        output.push_back(entry->first);
}

vector<TranslationOption> PhraseTable::GetTranslationOptions(const vector<wid_t> &phrase, context_t *context) {
    vector<sample_t> samples;
    self->index->GetRandomSamples(phrase, self->numberOfSamples, samples, context);

    vector<TranslationOption> result;
    MakeOptions(self->index, self->aligner, phrase, samples, result);

    return result;
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
                MakeOptions(self->index, self->aligner, phrase, samples, options);

                ttable[phrase] = options;
            }
        }

        delete collector;
    }

    return ttable;
}
