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

void PhraseTable::Delete(const updateid_t &id, const domain_t domain) {
    //TODO: Not implemented yet
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

/* Translation Options scoring */

static void GetLexicalScores(Aligner *aligner, const vector<wid_t> &phrase, const TranslationOption &option,
                             float &fwdScore, float &bwdScore) {
    vector<vector<float>> fwdWordProb(option.targetPhrase.size());
    vector<vector<float>> bwdWordProb(phrase.size());
    size_t sSize = phrase.size();
    size_t tSize = option.targetPhrase.size();

    // Computes the lexical probabilities on the best alignment only
    for (auto a = option.alignment.begin(); a != option.alignment.end(); ++a) {
        wid_t sWord = phrase[a->first];
        wid_t tWord = option.targetPhrase[a->second];
        fwdWordProb[a->second].push_back(aligner->GetForwardProbability(sWord, tWord));  // P(tWord | sWord)
        bwdWordProb[a->first].push_back(aligner->GetBackwardProbability(sWord, tWord));  // P(sWord | tWord)

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
        // should never happen that tmpProb <= 0
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

        // should never happen that tmpProb <= 0
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

    // Compute frequency-based and (possibly) lexical-based scores for all options
    // create the actual Translation option objects, setting the "best" alignment.
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
        option.orientations = entry->GetOrientations();

        if (aligner)
            GetLexicalScores(aligner, phrase, option, fwdLexScore, bwdLexScore);

        option.scores[ForwardProbabilityScore] = fwdScore;
        option.scores[BackwardProbabilityScore] = min(0.f, bwdScore);
        option.scores[ForwardLexicalScore] = fwdLexScore;
        option.scores[BackwardLexicalScore] = bwdLexScore;

        output.push_back(option);
    }
}

/* SAPT methods */

vector<TranslationOption> PhraseTable::GetTranslationOptions(const vector<wid_t> &phrase, context_t *context) {
    vector<sample_t> samples;
    self->index->GetRandomSamples(phrase, self->numberOfSamples, samples, context);

    vector<TranslationOption> result;
    MakeTranslationOptions(self->index, self->aligner, phrase, samples, result);

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
                MakeTranslationOptions(self->index, self->aligner, phrase, samples, options);

                ttable[phrase] = options;
            }
        }
       delete collector;
    }

    return ttable;
}
