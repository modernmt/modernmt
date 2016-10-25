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


typedef vector<bool> bitvector;

const size_t po_other = mmt::sapt::kTONoneOrientation;
// check if min and max in the alignment vector v are within the
// bounds LFT and RGT and update the actual bounds L and R; update
// the total count of alignment links in the underlying phrase
// pair
bool
check(vector<ushort> const& v, // alignment row/column
      size_t const LFT, size_t const RGT, // hard limits
      ushort& L, ushort& R, size_t& count) // current bounds, count
{
    if (v.size() == 0) return 0;
    if (L > v.front() && (L=v.front()) < LFT) return false;
    if (R < v.back()  && (R=v.back())  > RGT) return false;
    count += v.size();
    return true;
}

/// return number of alignment points in box, -1 on failure
int
expand_block(vector<vector<ushort> > const& row2col,
             vector<vector<ushort> > const& col2row,
             size_t       row, size_t       col, // seed coordinates
             size_t const TOP, size_t const LFT, // hard limits
             size_t const BOT, size_t const RGT, // hard limits
             ushort* top = NULL, ushort* lft = NULL,
             ushort* bot = NULL, ushort* rgt = NULL) // store results
{
    if (row < TOP || row > BOT || col < LFT || col > RGT) return -1;
    assert(row >= row2col.size());
    assert(col >= col2row.size());

    // ====================================================
    // tables grow downwards, so TOP is smaller than BOT!
    // ====================================================

    ushort T, L, B, R; // box dimensions

    // if we start on an empty cell, search for the first alignment point
    if (row2col[row].size() == 0 && col2row[col].size() == 0)
    {
        if      (row == TOP) while (row < BOT && !row2col[++row].size());
        else if (row == BOT) while (row > TOP && !row2col[--row].size());

        if      (col == LFT) while (col < RGT && !col2row[++col].size());
        else if (col == RGT) while (col > RGT && !col2row[--col].size());

        if (row2col[row].size() == 0 && col2row[col].size() == 0)
            return 0;
    }
    if (row2col[row].size() == 0)
        row = col2row[col].front();
    if (col2row[col].size() == 0)
        col = row2col[row].front();

    if ((T = col2row[col].front()) < TOP) return -1;
    if ((B = col2row[col].back())  > BOT) return -1;
    if ((L = row2col[row].front()) < LFT) return -1;
    if ((R = row2col[row].back())  > RGT) return -1;

    if (B == T && R == L) return 1;

    // start/end of row / column coverage:
    ushort rs = row, re = row, cs = col, ce = col;
    int ret = row2col[row].size();
    for (size_t tmp = 1; tmp; ret += tmp)
    {
        tmp = 0;;
        while (rs>T) if (!check(row2col[--rs],LFT,RGT,L,R,tmp)) return -1;
        while (re<B) if (!check(row2col[++re],LFT,RGT,L,R,tmp)) return -1;
        while (cs>L) if (!check(col2row[--cs],TOP,BOT,T,B,tmp)) return -1;
        while (ce<R) if (!check(col2row[++ce],TOP,BOT,T,B,tmp)) return -1;
    }
    if (top) *top = T;
    if (bot) *bot = B;
    if (lft) *lft = L;
    if (rgt) *rgt = R;
    return ret;
}

ReorderingType
find_po_fwd(vector<vector<ushort> >& a1,
            vector<vector<ushort> >& a2,
            size_t s1, size_t e1,
            size_t s2, size_t e2)
{
    if (e2 == a2.size()) { // end of target sentence
        return mmt::sapt::kTOMonotonicOrientation;
    }
    size_t y = e2, L = e2, R = a2.size()-1; // won't change
    size_t x = e1, T = e1, B = a1.size()-1;
    if (e1 < a1.size() && expand_block(a1,a2,x,y,T,L,B,R) >= 0) {
        return mmt::sapt::kTOMonotonicOrientation;
    }
    B = x = s1-1; T = 0;
    if (s1 && expand_block(a1,a2,x,y,T,L,B,R) >= 0) {
        return mmt::sapt::kTOSwapOrientation;
    }
    while (e2 < a2.size() && a2[e2].size() == 0) ++e2;
    if (e2 == a2.size()) { // should never happen, actually
        return mmt::sapt::kTONoneOrientation;
    }
    if (a2[e2].back() < s1) {
        return mmt::sapt::kTODiscontinuousLeftOrientation;
    }
    if (a2[e2].front() >= e1) {
        return mmt::sapt::kTODiscontinuousRightOrientation;
    }
    return mmt::sapt::kTONoneOrientation;
}

ReorderingType
find_po_bwd(vector<vector<ushort> >& a1,
            vector<vector<ushort> >& a2,
            size_t s1, size_t e1,
            size_t s2, size_t e2)
{
    if (s1 == 0 && s2 == 0){
        return mmt::sapt::kTOMonotonicOrientation;
    }
    if (s2 == 0){
        return mmt::sapt::kTODiscontinuousRightOrientation;
    }
    if (s1 == 0){
        return mmt::sapt::kTODiscontinuousLeftOrientation;
    }
    size_t y = s2-1, L = 0, R = s2-1; // won't change
    size_t x = s1-1, T = 0, B = s1-1;
    if (expand_block(a1,a2,x,y,T,L,B,R) >= 0) {
        return mmt::sapt::kTOMonotonicOrientation;
    }
    T = x = e1; B = a1.size()-1;
    if (expand_block(a1,a2,x,y,T,L,B,R) >= 0) {
        return mmt::sapt::kTOSwapOrientation;
    }
    while (s2-- && a2[s2].size() == 0);

    mmt::sapt::ReorderingType ret;
    ret = (a2[s2].size()  ==  0 ? po_other :
           a2[s2].back()   < s1 ? mmt::sapt::kTODiscontinuousRightOrientation :
           a2[s2].front() >= e1 ? mmt::sapt::kTODiscontinuousLeftOrientation :
           po_other);
    return ret;
}


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
                               const alignment_t &allAlignment, const alignment_t &inBoundAlignment, const vector<bool> &targetAligned,
                               int sourceStart, int sourceEnd, int targetStart, int targetEnd,
                               optionsmap_t &outOptions) {
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
/*
            //compute orientation for this phrase pair
            ReorderingType forwardOrientation = kTOMonotonicOrientation; //dummy computation
            ReorderingType backwardOrientation = kTOSwapOrientation; //dummy computation
            */


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

static void GetTranslationOptionsFromSample(const vector<wid_t> &sourcePhrase, const sample_t &sample, optionsmap_t &outOptions) {
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

        ExtractPhrasePairs(sample.source, sample.target, sample.alignment, inBoundsAlignment, targetAligned,
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
        ((TranslationOption&) entry->first).SetBestAlignment();
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
                MakeOptions(self->index, self->aligner, phrase, samples, options);

                ttable[phrase] = options;
            }
        }

        delete collector;
    }

    return ttable;
}