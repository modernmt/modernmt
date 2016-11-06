//
// Created by Davide  Caroselli on 19/10/16.
//

#include <util/hashutils.h>
#include <cassert>
#include "TranslationOptionBuilder.h"

using namespace mmt;
using namespace mmt::sapt;


typedef vector<bool> bitvector;

// returns lowerBound <= var <= upperBound
#define InRange(lowerBound, var, upperBound) (lowerBound <= var && var <= upperBound)

static inline int compare_alignments(const mmt::alignment_t &a, const mmt::alignment_t &b) {
    if (a.size() != b.size())
        return (int) (a.size() - b.size());

    for (size_t i = 0; i < a.size(); ++i) {
        if (a[i].first != b[i].first) return a[i].first - b[i].first;
        if (a[i].second != b[i].second) return a[i].second - b[i].second;
    }

    return 0;
}

void TranslationOptionBuilder::ExtractOptions(const vector<wid_t> &sourceSentence, const vector<wid_t> &targetSentence,
                               const alignment_t &allAlignment, const alignment_t &inBoundAlignment, const vector<bool> &targetAligned,
                               int sourceStart, int sourceEnd, int targetStart, int targetEnd,
                               optionsmap_t &map, bool &isValidOption) {

    int ts = targetStart;
    while (true) {
        int te = targetEnd;
        while (true) {
            vector<wid_t> targetPhrase(targetSentence.begin() + ts, targetSentence.begin() + te + 1);

            alignment_t shiftedAlignment = inBoundAlignment;
            //reset the word positions within the phrase pair, regardless the sentence context
            for (auto a = shiftedAlignment.begin(); a != shiftedAlignment.end(); ++a) {
                a->first -= sourceStart;
                a->second -= ts;
            }

            //determine fwd and bwd phrase orientation
            size_t slen1; // length of source sentence in case of forward
            size_t slen2; // length of target sentence in case of forward
            bool flip = 0;
            if (flip) {
                slen1 = targetSentence.size();
                slen2 = sourceSentence.size();
            } else {
                slen1 = sourceSentence.size();
                slen2 = targetSentence.size();
            }

            size_t start = sourceStart;
            size_t stop = sourceEnd + 1;

            std::vector<std::vector<ushort> > aln1(slen1); //long as the source sentence (or target if flipped)
            std::vector<std::vector<ushort> > aln2(slen2); //long as the target sentence (or source if flipped)
            bitvector forbidden(slen2);

            size_t src, trg;
            size_t lft = forbidden.size();
            size_t rgt = 0;

            for (auto align = allAlignment.begin(); align != allAlignment.end(); ++align) {

                if (flip) {
                    src = align->second;
                    trg = align->first;
                } else {
                    src = align->first;
                    trg = align->second;
                }

                assert(src < slen1);
                assert(trg < slen2);

                if (src < start || src >= stop) {
                    forbidden.at(trg) = true;
                } else {
                    lft = std::min(lft, trg);
                    rgt = std::max(rgt, trg);
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
            for (s1 = s2; s1 && !forbidden[s1 - 1]; --s1) { };
            size_t e1 = rgt + 1, e2;
            for (e2 = e1; e2 < forbidden.size() && !forbidden[e2]; ++e2) { };

            size_t fwd = kTONoneOrientation;
            size_t bwd = kTONoneOrientation;
            if (computeOrientation) {
                fwd = OrientationType::find_po_fwd(aln1, aln2, start, stop, s1, e2);
                bwd = OrientationType::find_po_bwd(aln1, aln2, start, stop, s1, e2);
            }

            auto builder = map.emplace(targetPhrase, targetPhrase);
            builder.first->second.Add(shiftedAlignment);
            builder.first->second.AddForwardOrientation(fwd);
            builder.first->second.AddBackwardOrientation(bwd);
            isValidOption = true;

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
                                       vector<TranslationOptionBuilder> &output, size_t &validSamples) {
    optionsmap_t map;

    for (auto sample = samples.begin(); sample != samples.end(); ++sample) { //loop over sampled sentence pairs
        // Create bool vector to know whether a target word is aligned.
        vector<bool> targetAligned(sample->target.size(), false);
        for (auto alignPoint = sample->alignment.begin(); alignPoint != sample->alignment.end(); ++alignPoint)
            targetAligned[alignPoint->second] = true;

        for (auto offset = sample->offsets.begin(); offset != sample->offsets.end(); ++offset) { //loop over offset of a sampled sentence pair
            TranslationOptionBuilder::Extract(sourcePhrase, *sample, *offset, targetAligned, map, validSamples);
        }
    }

    output.reserve(map.size());
    for (auto entry = map.begin(); entry != map.end(); ++entry)
        output.push_back(entry->second);
}

void TranslationOptionBuilder::Extract(const vector<wid_t> &sourcePhrase, const sample_t &sample, int offset, vector<bool> &targetAligned, optionsmap_t &map, size_t &validSamples) {
    // Search for source and target bounds
    int sourceStart = offset;
    int sourceEnd = (int) (sourceStart + sourcePhrase.size() - 1);

    int targetStart = (int) (sample.target.size() - 1);
    int targetEnd = -1;

    for (auto alignPoint = sample.alignment.begin(); alignPoint != sample.alignment.end(); ++alignPoint) {
        if (InRange(sourceStart, alignPoint->first, sourceEnd)) {
            targetStart = min((int) alignPoint->second, targetStart);
            targetEnd = max((int) alignPoint->second, targetEnd);
        }
    }

    // Check target bounds validity
    if (targetEnd - targetStart < 0)
        return;

    if (targetEnd < 0) // 0-based indexing.
        return;

    // Collect alignment points within the block (sourceStart,targetStart)-(sourceEnd,targetEnd)
    // Check whether any alignment point exists outside the block,
    // but with either the source position within the source inBounds
    // or tha target position within the target inBounds
    // In this case do not proceed with the option extraction

    alignment_t inBoundsAlignment;
    bool isValidAlignment = true;
    for (auto alignPoint = sample.alignment.begin(); alignPoint != sample.alignment.end(); ++alignPoint) {
        bool srcInbound = InRange(sourceStart, alignPoint->first, sourceEnd);
        bool trgInbound = InRange(targetStart, alignPoint->second, targetEnd);

        if (srcInbound != trgInbound) {
            isValidAlignment = false;
            break;
        }

        if (srcInbound)
            inBoundsAlignment.push_back(*alignPoint);

    }

    if (isValidAlignment) {
        bool isValidOption = false;
        // Extract the TranslationOptions
        TranslationOptionBuilder::ExtractOptions(sample.source, sample.target, sample.alignment, inBoundsAlignment,
                                                     targetAligned,
                                                     sourceStart, sourceEnd, targetStart, targetEnd, map,
                                                     isValidOption);
        if (isValidOption)
            ++validSamples;
    }
}
