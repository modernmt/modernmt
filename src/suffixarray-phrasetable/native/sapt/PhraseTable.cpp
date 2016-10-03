//
// Created by Davide  Caroselli on 27/09/16.
//

#include <algorithm>
#include <suffixarray/SuffixArray.h>

#include "PhraseTable.h"
#include "UpdateManager.h"

using namespace mmt;
using namespace mmt::sapt;

struct PhraseTable::pt_private {
    SuffixArray *index;
    UpdateManager *updates;
};

PhraseTable::PhraseTable(const string &modelPath, const Options &options) {
    self = new pt_private();
    self->index = new SuffixArray(modelPath, options.prefix_length);
    self->updates = new UpdateManager(self->index, options.update_buffer_size, options.update_max_delay);
    numScoreComponent = options.numScoreComponent;
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

void *PhraseTable::__GetSuffixArray() {
    return self->index;
}
void PhraseTable::GetTargetPhraseCollection(const vector<wid_t> &sourcePhrase, vector<TranslationOption> &outOptions, context_t *context) {

    cout << "sourcePhrase.size():" << sourcePhrase.size()  << endl;
    std::cerr << "SourcePhrase:|";
    for (auto w = sourcePhrase.begin(); w != sourcePhrase.end(); ++w) { std::cerr << *w << " "; }
    std::cerr << "|" << std::endl;

    vector<sample_t> samples;
    self->index->GetRandomSamples(sourcePhrase, 10, samples, context);

    cout << "Found " << samples.size() << " samples" << endl;

    std::cerr << "Found " << samples.size()  << " samples" << std::endl;

    for (auto sample=samples.begin(); sample != samples.end(); ++ sample){
/*        std::cerr << "Source:|";
        for (auto w = sample->source.begin(); w != sample->source.end(); ++w) { std::cerr << *w << " "; }
        std::cerr << "| Target:|";
        for (auto w = sample->target.begin(); w != sample->target.end(); ++w) { std::cerr << *w << " "; }
        std::cerr << "| Offset:|";
        for (auto o = sample->offsets.begin(); o != sample->offsets.end(); ++o) { std::cerr << *o << " "; }
        std::cerr << "| Alignemnt:|";
        for (auto a = sample->alignment.begin(); a != sample->alignment.end(); ++a) { std::cerr << a->first << "-" << a->second << " "; }
        std::cerr << "|";
        std::cerr << " Domain:|" << sample->domain << "|" << std::endl;*/

        //GetTranslationOptions(sourcePhrase, *sample, outOptions);
        //GetTranslationOptions(sourcePhrase, outOptions);
        GetTranslationOptions(sourcePhrase, sample->source, sample->target, sample->alignment, sample->offsets, outOptions);
    }
    std::cerr << "Found " << outOptions.size()  << " options" << std::endl;

    //loop over all Options and score them
};

void PhraseTable::GetTranslationOptions(const vector<wid_t> &sourcePhrase,
                                        const std::vector<wid_t> &sourceSentence,
                                        const std::vector<wid_t> &targetSentence,
                                        const alignment_t &alignment,
                                        const std::vector<length_t> &offsets,
                                        std::vector<TranslationOption> &outOptions){

//void PhraseTable::GetTranslationOptions(const vector<wid_t> &sourcePhrase, sample_t &sample, vector<TranslationOption> &outOptions) {

// Keeps a vector to know whether a target word is aligned.
    std::vector<bool> targetAligned(targetSentence.size(),false);

    for (auto alignPoint = alignment.begin(); alignPoint != alignment.end(); ++alignPoint){
        targetAligned[alignPoint->second] = true;
    }

    std::cerr << "targetAligned.size:" << targetAligned.size()  << std::endl;

    for (auto offset = offsets.begin(); offset != offsets.end(); ++ offset){ //for each occurrence of the source in the sampled sentence pair
        std::cerr << "offset:" << *offset  << std::endl;

        //get source position lowerBound  and  upperBound
        length_t sourceStart = *offset; // lowerBound is always larger than or equal to 0
        length_t sourceEnd = sourceStart + sourcePhrase.size() - 1; // upperBound is always larger than or equal to 0, because sourcePhrase.size()>=1

        // find the minimally matching foreign phrase
        int targetStart = targetSentence.size() - 1;
        int targetEnd = -1;

        for (auto alignPoint = alignment.begin(); alignPoint != alignment.end(); ++alignPoint) {
//            std::cerr << "\n\nalignPoint->first:" << alignPoint->first << " alignPoint->second:" << alignPoint->second << std::endl;

            if ( (alignPoint->first >= sourceStart ) && (alignPoint->first <= sourceEnd ) ){
                targetStart = std::min((int) alignPoint->second, targetStart);
                targetEnd = std::max((int) alignPoint->second, targetEnd);
            }

        }

        std::cerr << "calling ExtractPhrasePairs with " << std::endl;
        std::cerr << "sourceStart:" << sourceStart << " sourceEnd:" << sourceEnd << " targetStart:" << targetStart << " targetEnd:" << targetEnd << std::endl;

        ExtractPhrasePairs(sourceSentence, targetSentence, alignment, targetAligned, sourceStart, sourceEnd, targetStart, targetEnd, outOptions); //add all extracted phrase pairs into outOptions

    }
}


void PhraseTable::ExtractPhrasePairs(const std::vector<wid_t> &sourceSentence,
                                     const std::vector<wid_t> &targetSentence,
                                     const alignment_t &alignment,
                                     const std::vector<bool> &targetAligned,
                                     length_t sourceStart, length_t sourceEnd, int targetStart, int targetEnd,
                                     std::vector<TranslationOption> &outOptions) {

    if (targetEnd < 0) // 0-based indexing.
        return;

// Check if alignment points are consistent. if yes, copy
    alignment_t currentAlignments;
    for (auto alignPoint = alignment.begin(); alignPoint != alignment.end(); ++alignPoint) {

        if (((alignPoint->first >= sourceStart) && (alignPoint->first <= sourceEnd)) &&
            ((alignPoint->second < targetStart) && (alignPoint->second > targetEnd))) {
            return;
        }
        currentAlignments.push_back(*alignPoint);
    }

    int ts = targetStart;
    while (true) {
        int te = targetEnd;
        while (true) {
// add phrase pair ([e_start, e_end], [fs, fe]) to set E
            TranslationOption option(numScoreComponent);
            option.targetPhrase.insert(option.targetPhrase.begin(), targetSentence.begin() + ts,
                                       targetSentence.begin() + te + 1);
            option.alignment = currentAlignments;

            outOptions.push_back(option);
            te += 1;
// if fe is in word alignment or out-of-bounds
            if (targetAligned[te] || te == targetSentence.size()) {
                break;
            }
        }
        ts -= 1;
// if fs is in word alignment or out-of-bounds
        if (targetAligned[ts] || ts < 0) {
            break;
        }
    }
}

void PhraseTable::NormalizeContext(context_t *context) {
    context_t ret;
    float total = 0.0;

    for (auto it = context->begin(); it != context->end(); ++it) {
        //todo:: can it happen that the domain is empty?
        total += it->score;
    }

    if (total == 0.0)
        total = 1.0f;

    for (auto it = context->begin(); it != context->end(); ++it) {
        //todo:: can it happen that the domain is empty?
        it->score /= total;

        ret.push_back(*it);
    }

    // replace new vector into old vector
    context->clear();
    //todo: check if the following insert is correct
    context->insert(context->begin(), ret.begin(), ret.end());
}
