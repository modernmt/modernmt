//
// Created by Davide  Caroselli on 19/10/16.
//

#include <util/hashutils.h>
#include <cassert>
#include "TranslationOptionBuilder.h"

using namespace mmt;
using namespace mmt::sapt;

// returns lowerBound <= var <= upperBound
#define InRange(lowerBound, var, upperBound) (lowerBound <= var && var <= upperBound)

typedef unordered_map<vector<wid_t>, TranslationOptionBuilder, phrase_hash> optionsmap_t;

static inline int compare_alignments(const mmt::alignment_t &a, const mmt::alignment_t &b) {
    if (a.size() != b.size())
        return (int) (a.size() - b.size());

    for (size_t i = 0; i < a.size(); ++i) {
        if (a[i].first != b[i].first) return a[i].first - b[i].first;
        if (a[i].second != b[i].second) return a[i].second - b[i].second;
    }

    return 0;
}

TranslationOptionBuilder::TranslationOptionBuilder(const vector<wid_t> &phrase) : phrase(phrase), count(0) {}

const alignment_t &TranslationOptionBuilder::GetBestAlignment() const {
    assert(!alignments.empty());

    auto bestEntry = alignments.begin();

    for (auto entry = alignments.begin(); entry != alignments.end(); ++entry) {
        if (entry->second > bestEntry->second ||
            (entry->second == bestEntry->second && compare_alignments(entry->first, bestEntry->first) < 0))
            bestEntry = entry;
    }

    return bestEntry->first;
}

void TranslationOptionBuilder::Add(const alignment_t &alignment) {
    alignments[alignment]++;
    count++;
}

void TranslationOptionBuilder::Extract(const vector<wid_t> &sourcePhrase, const vector<sample_t> &samples,
                                       vector<TranslationOptionBuilder> &output) {
    optionsmap_t map;

    for (auto sample = samples.begin(); sample != samples.end(); ++sample) {
        // Create bool vector to know whether a target word is aligned.
        vector<bool> targetAligned(sample->target.size(), false);
        for (auto alignPoint = sample->alignment.begin(); alignPoint != sample->alignment.end(); ++alignPoint)
            targetAligned[alignPoint->second] = true;

        for (auto offset = sample->offsets.begin(); offset != sample->offsets.end(); ++offset) {
            // Search for source and target bounds
            int sourceStart = *offset;
            int sourceEnd = (int) (sourceStart + sourcePhrase.size() - 1);

            int targetStart = (int) (sample->target.size() - 1);
            int targetEnd = -1;

            for (auto alignPoint = sample->alignment.begin(); alignPoint != sample->alignment.end(); ++alignPoint) {
                if (InRange(sourceStart, alignPoint->first, sourceEnd)) {
                    targetStart = min((int) alignPoint->second, targetStart);
                    targetEnd = max((int) alignPoint->second, targetEnd);
                }
            }

            // Check target bounds validity
            if (targetEnd - targetStart < 0)
                continue;

            // Collect alignments within source and target bounds.
            // A translation option could be invalid if one of its points is outside
            // the bounds of the source or target phrase.
            alignment_t inBoundsAlignment;
            bool isValidOption = true;

            for (auto alignPoint = sample->alignment.begin(); alignPoint != sample->alignment.end(); ++alignPoint) {
                if (InRange(sourceStart, alignPoint->first, sourceEnd)) {
                    if (InRange(targetStart, alignPoint->second, targetEnd)) {
                        inBoundsAlignment.push_back(*alignPoint);
                    } else {
                        isValidOption = false;
                        break;
                    }
                } else if (InRange(targetStart, alignPoint->second, targetEnd)) {
                    if (InRange(sourceStart, alignPoint->first, sourceEnd)) {
                        inBoundsAlignment.push_back(*alignPoint);
                    } else {
                        isValidOption = false;
                        break;
                    }
                }
            }

            if (!isValidOption)
                continue;

            // Extract the TranslationOptions
            for (int ts = targetStart; ts >= 0; --ts) {
                if (ts < targetStart && targetAligned[ts]) break;

                for (int te = targetEnd; te < (int) sample->target.size(); ++te) {
                    if (te > targetEnd && targetAligned[te]) break;

                    vector<wid_t> targetPhrase(sample->target.begin() + ts, sample->target.begin() + te + 1);

                    // reset the word positions within the phrase pair
                    alignment_t shiftedAlignment = inBoundsAlignment;
                    for (auto a = shiftedAlignment.begin(); a != shiftedAlignment.end(); ++a) {
                        a->first -= sourceStart;
                        a->second -= ts;
                    }

                    auto builder = map.emplace(targetPhrase, targetPhrase);
                    builder.first->second.Add(shiftedAlignment);
                }
            }
        }
    }

    output.reserve(map.size());
    for (auto entry = map.begin(); entry != map.end(); ++entry)
        output.push_back(entry->second);
}
