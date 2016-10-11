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
    mmt::Aligner *aligner;
};

PhraseTable::PhraseTable(const string &modelPath, const Options &options, mmt::Aligner *alignerModel) {
    self = new pt_private();
    self->index = new SuffixArray(modelPath, options.prefix_length);
    self->updates = new UpdateManager(self->index, options.update_buffer_size, options.update_max_delay);
    self->aligner = alignerModel;
    numScoreComponent = 4;
    debug=false;
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
void PhraseTable::GetTargetPhraseCollection(const vector<wid_t> &sourcePhrase, size_t limit, vector<TranslationOption> &optionsVec, context_t *context) {
    cerr << "void PhraseTable::GetTargetPhraseCollection(...) sourcePhrase.size():" << sourcePhrase.size() << endl;
    for (size_t i=0; i< sourcePhrase.size(); ++i){
        cerr << "void PhraseTable::GetTargetPhraseCollection(...) sourcePhrase[" << i << "]:" << sourcePhrase[i] << endl;
    }

    OptionsMap_t optionsMap;

    vector<sample_t> samples;
    self->index->GetRandomSamples(sourcePhrase, limit, samples, context);

    cerr << "Found " << samples.size() << " samples (maximum was " << limit << ")" << endl;

    for (auto sample=samples.begin(); sample != samples.end(); ++ sample){
        //cout << *sample << endl;
        //std::cout << "sample->source.size():" << sample->source.size() << " sample->target.size():" << sample->target.size()<< std::endl;
        //GetTranslationOptions(sourcePhrase, sample->source, sample->target, sample->alignment, sample->offsets, optionsVec);
        GetTranslationOptions(sourcePhrase, sample->source, sample->target, sample->alignment, sample->offsets, optionsMap);
    }
    cerr << "void PhraseTable::GetTargetPhraseCollection(...) sourcePhrase.size():" << sourcePhrase.size() << " optionsMap.size():" << optionsMap.size() << endl;

    //loop over all Options and score them
    ScoreTranslationOptions(optionsMap, sourcePhrase, samples.size());

    //transform the map into a vector
    for (auto entry = optionsMap.begin(); entry != optionsMap.end(); ++entry) {
        optionsVec.push_back(entry->first);
    }
};

void PhraseTable::ScoreTranslationOptions(OptionsMap_t &optionsMap, const vector<wid_t> &sourcePhrase, size_t NumberOfSamples){

    size_t SampleSourceFrequency = 0;
    for (auto entry = optionsMap.begin(); entry != optionsMap.end(); ++ entry) {
        SampleSourceFrequency += entry->second;
    }
    if (debug) std::cerr  << "Found " << SampleSourceFrequency  << " option tokens and " << optionsMap.size() << " option types"<< std::endl;
    //std::cerr << "Found " << SampleSourceFrequency  << " option tokens and " << optionsMap.size() << " option types"<< std::endl;

    size_t GlobalSourceFrequency = self->index->CountOccurrences(true, sourcePhrase);
    for (auto entry = optionsMap.begin(); entry != optionsMap.end(); ++ entry) {
        //std::cerr << "void PhraseTable::ScoreTranslationOptions(....) options is:|" << entry->first << "|" << std::endl;
        size_t GlobalTargetFrequency = self->index->CountOccurrences(false, entry->first.targetPhrase);

/*
	std::cerr << "options is:|" << entry->first
                   << " Frequency:" << entry->second << " SampleSourceFrequency:" << SampleSourceFrequency
                   << " GlobalSourceFrequency:"<< GlobalSourceFrequency << " GlobalTargetFrequency:" << GlobalTargetFrequency
                   <<  " NumberOfSamples:"<< NumberOfSamples << std::endl;
*/
        std::vector<float> scores(numScoreComponent);

        //set the forward and backward frequency-based scores of the current option
        scores[0] = log( (float) entry->second / SampleSourceFrequency );
        //scores[1] = ((float) entry->second / NumberOfSamples) * ((float) GlobalSourceFrequency / GlobalTargetFrequency);
        scores[1] = log(((float) entry->second / SampleSourceFrequency) * ((float) GlobalSourceFrequency / GlobalTargetFrequency) );
        //std::cerr << "scores[1]:"<< scores[1] << std::endl;
        scores[1] = std::min(scores[1], (float) 0.0);  //thresholded to log(1.0)

        //set the forward and backward lexical scores of the current option
        scores[2] = 0.0;
        scores[3] = 0.0;
        //std::cerr << "void PhraseTable::ScoreTranslationOptions(....) BEFORE GetLexicalScores(sourcePhrase, entry->first, scores[2], scores[3])"<< std::endl;
        GetLexicalScores(sourcePhrase, entry->first, scores[2], scores[3]);
        //std::cerr << "void PhraseTable::ScoreTranslationOptions(....) AFTER GetLexicalScores(sourcePhrase, entry->first, scores[2], scores[3]) scores[2]:" << scores[2] << " scores[3]:" << scores[3] << std::endl;

        ((TranslationOption*) &entry->first)->SetScores(scores);
    }
}

void PhraseTable::GetLexicalScores(const vector<wid_t> &sourcePhrase, const TranslationOption &option, float &fwdScore, float &bwdScore){
    if (self->aligner) { // if the AlignmentModel is provided, compute the lexical probabilities
        std::vector<std::vector<float> > fwdWordProb(option.targetPhrase.size());
        std::vector<std::vector<float> > bwdWordProb(sourcePhrase.size());
        size_t sSize = sourcePhrase.size();
        size_t tSize = option.targetPhrase.size();
        //std::cerr << "void PhraseTable::GetLexicalScores(...) sSize:" << sSize << " tSize:" << tSize << std::endl;
        for (auto a = option.alignment.begin(); a != option.alignment.end(); ++a) {
            //std::cerr << "void PhraseTable::GetLexicalScores(...) a->first:" << a->first << " a->second:" << a->second << std::endl;
            wid_t sWord = sourcePhrase[a->first];
            wid_t tWord = option.targetPhrase[a->second];
            //std::cerr << "void PhraseTable::GetLexicalScores(...) sWord:" << sWord << " tWord:" << tWord << std::endl;
            fwdWordProb[a->second].push_back(self->aligner->GetForwardProbability(sWord, tWord));  //P(tWord | sWord)
            bwdWordProb[a->first].push_back(self->aligner->GetBackwardProbability(sWord, tWord));  //P(sWord | tWord)
            //std::cerr << "void PhraseTable::GetLexicalScores(...) fwdProb:" << self->aligner->GetForwardProbability(sWord, tWord) << " bwdProb:" << self->aligner->GetBackwardProbability(sWord, tWord) << std::endl;

        }
        fwdScore = 0.0;
        for (size_t ti = 0; ti < tSize; ++ti) {
            float tmpProb = 0.0;
            size_t tmpSize = fwdWordProb[ti].size();
            //std::cerr << "void PhraseTable::GetLexicalScores(...) ti:" << ti << " tmpSize:" << tmpSize << " fwdWordProb[ti].size():" << fwdWordProb[ti].size() << std::endl;

            if (tmpSize > 0) {
                for (size_t i = 0; i < tmpSize; ++i) {
                    //std::cerr << "void PhraseTable::GetLexicalScores(...) ti: " << ti << " i:" << i << " fwdWordProb[ti][i]:" << fwdWordProb[ti][i] << std::endl;
                    tmpProb += fwdWordProb[ti][i];
                    //std::cerr << "void PhraseTable::GetLexicalScores(...) tmpProb:" << tmpProb << std::endl;
                }
                tmpProb /= tmpSize;
            } else {
                //std::cerr << "void PhraseTable::GetLexicalScores(...) ti:" << ti << " NullProb[ti]:" << self->aligner->GetTargetNullProbability(option.targetPhrase[ti]) << std::endl;
                tmpProb = self->aligner->GetTargetNullProbability(option.targetPhrase[ti]);
                //std::cerr << "void PhraseTable::GetLexicalScores(...) tmpProb:" << tmpProb << std::endl;
            }
            //std::cerr << "void PhraseTable::GetLexicalScores(...) FINAL tmpProb:" << tmpProb << std::endl;
            //should never happen that tmpProb <= 0
            fwdScore += (tmpProb <= 0.0) ? -9 : log(tmpProb);
            //std::cerr << "void PhraseTable::GetLexicalScores(...) fwdScore:" << fwdScore << std::endl;
        }
        //std::cerr << "void PhraseTable::GetLexicalScores(...) FINAL fwdScore:" << fwdScore << std::endl;

        bwdScore = 0.0;
        for (size_t si = 0; si < sSize; ++si) {
            float tmpProb = 0.0;
            size_t tmpSize = bwdWordProb[si].size();
            //std::cerr << "void PhraseTable::GetLexicalScores(...) si:" << si << " tmpSize:" << tmpSize << " bwdWordProb[si].size():" << bwdWordProb[si].size() << std::endl;
            if (tmpSize > 0) {
                for (size_t i = 0; i < tmpSize; ++i) {
                    //std::cerr << "void PhraseTable::GetLexicalScores(...) si: " << si << " i:" << i << " bwdWordProb[si][i]:" << bwdWordProb[si][i] << std::endl;
                    tmpProb += bwdWordProb[si][i];
                   //std::cerr << "void PhraseTable::GetLexicalScores(...) tmpProb:" << tmpProb << std::endl;

                }
                tmpProb /= tmpSize;
            } else {
                //std::cerr << "void PhraseTable::GetLexicalScores(...) si:" << si << " NullProb[si]:" << self->aligner->GetTargetNullProbability(sourcePhrase[si]) << std::endl;
                tmpProb = self->aligner->GetSourceNullProbability(sourcePhrase[si]);
                //std::cerr << "void PhraseTable::GetLexicalScores(...) tmpProb:" << tmpProb << std::endl;
            }
            //std::cerr << "void PhraseTable::GetLexicalScores(...) FINAL tmpProb:" << tmpProb << std::endl;
            //should never happen that tmpProb <= 0
            bwdScore += (tmpProb <= 0.0) ? -9 : log(tmpProb);
            //std::cerr << "void PhraseTable::GetLexicalScores(...) bwdScore:" << bwdScore << std::endl;
        }
        //std::cerr << "void PhraseTable::GetLexicalScores(...) FINAL bwdScore:" << bwdScore << std::endl;
    }
}
/*
void PhraseTable::GetLexicalScores(const vector<wid_t> &sourcePhrase, const TranslationOption &option, float &fwdScore, float &bwdScore){
    if (self->aligner) { // if the AlignmentModel is provided, compute the lexical probabilities
        std::vector<std::vector<float> > fwdWordProb(sourcePhrase.size());
        std::vector<std::vector<float> > bwdWordProb(option.targetPhrase.size());
        size_t sSize = sourcePhrase.size();
        size_t tSize = option.targetPhrase.size();
        //std::cerr << "void PhraseTable::GetLexicalScores(...) sSize:" << sSize << " tSize:" << tSize << std::endl;
        for (auto a = option.alignment.begin(); a != option.alignment.end(); ++a) {
            wid_t sWord = sourcePhrase[a->first];
            wid_t tWord = option.targetPhrase[a->second];
            fwdWordProb[a->first].push_back(self->aligner->GetForwardProbability(sWord, tWord));
            bwdWordProb[a->second].push_back(self->aligner->GetBackwardProbability(sWord, tWord));
            //std::cerr << "void PhraseTable::GetLexicalScores(...) a->first:" << a->first << " a->second:" << a->second << std::endl;
            //std::cerr << "void PhraseTable::GetLexicalScores(...) fwdProb:" << self->aligner->GetForwardProbability(sWord, tWord) << " bwdProb:" << self->aligner->GetBackwardProbability(sWord, tWord) << std::endl;
        }
        fwdScore = 0.0;
        for (size_t si = 0; si < sSize; ++si) {
            float tmp = 0.0;
            //std::cerr << "void PhraseTable::GetLexicalScores(...) si:" << si << " fwdWordProb[si].size():" << fwdWordProb[si].size() << std::endl;
            if (fwdWordProb[si].size() > 0) {
                for (size_t i = 0; i < fwdWordProb[si].size(); ++i) {
                    //std::cerr << "void PhraseTable::GetLexicalScores(...) si:" << si << " i:" << i << " fwdWordProb[si][i]:" << fwdWordProb[si][i] << std::endl;
                    tmp += fwdWordProb[si][i];
                    //std::cerr << "void PhraseTable::GetLexicalScores(...) tmp:" << tmp << std::endl;
                }
            } else {
                //std::cerr << "void PhraseTable::GetLexicalScores(...) si:" << si << " NullProb[si][i]:" << self->aligner->GetForwardNullProbability(sourcePhrase[si]) << std::endl;
                tmp = self->aligner->GetForwardNullProbability(sourcePhrase[si]);
                //std::cerr << "void PhraseTable::GetLexicalScores(...) tmp:" << tmp << std::endl;
            }
            //std::cerr << "void PhraseTable::GetLexicalScores(...) FINAL tmp:" << tmp << std::endl;
            if (tmp <= 0.0){
                tmp = -9;  //should never happen
            } else {
                tmp=log(tmp);
            }
            fwdScore += tmp;
            //std::cerr << "void PhraseTable::GetLexicalScores(...) FINAL fwdScore:" << fwdScore << std::endl;
        }
        //std::cerr << "void PhraseTable::GetLexicalScores(...) fwdScore:" << fwdScore << std::endl;

        bwdScore = 0.0;
        for (size_t ti = 0; ti < tSize; ++ti) {
            float tmp = 0.0;
            //std::cerr << "void PhraseTable::GetLexicalScores(...) ti:" << ti << " bwdWordProb[ti].size():" << bwdWordProb[ti].size() << std::endl;
            if (bwdWordProb[ti].size() > 0) {
                for (size_t i = 0; i < bwdWordProb[ti].size(); ++i) {
                    //std::cerr << "void PhraseTable::GetLexicalScores(...) i:" << i << " bwdWordProb[ti][i]:" << bwdWordProb[ti][i] << std::endl;
                    tmp += bwdWordProb[ti][i];
                }
            } else {
                tmp = self->aligner->GetBackwardNullProbability(option.targetPhrase[ti]);
            }
            if (tmp <= 0.0){
		tmp = -9;  //should never happen
            } else {
                tmp=log(tmp);
            }
            bwdScore += tmp;
            //std::cerr << "void PhraseTable::GetLexicalScores(...) FINAL bwdScore:" << bwdScore << std::endl;
        }
        //std::cerr << "void PhraseTable::GetLexicalScores(...) bwdScore:" << bwdScore << std::endl;
    }
}
*/

/*
float PhraseTable::GetForwardLexicalScore(length_t sourceWord, length_t targetWord){
    return 0.0;
}

float PhraseTable::GetBackwardLexicalScore(length_t sourceWord, length_t targetWord){
    return 0.0;
}*/


void PhraseTable::GetTranslationOptions(const vector<wid_t> &sourcePhrase,
                                        const std::vector<wid_t> &sourceSentence,
                                        const std::vector<wid_t> &targetSentence,
                                        const alignment_t &alignment,
                                        const std::vector<length_t> &offsets,
                                        OptionsMap_t &optionsMap){

//void PhraseTable::GetTranslationOptions(const vector<wid_t> &sourcePhrase, sample_t &sample, vector<TranslationOption> &outOptions) {

// Keeps a vector to know whether a target word is aligned.
    std::vector<bool> targetAligned(targetSentence.size(),false);

    //for (auto alignPoint = corrected_alignment.begin(); alignPoint != corrected_alignment.end(); ++alignPoint){
    for (auto alignPoint = alignment.begin(); alignPoint != alignment.end(); ++alignPoint){
        if ((alignPoint->first < sourceSentence.size()) && (alignPoint->second < targetSentence.size())) {
            targetAligned[alignPoint->second] = true;
        }
    }

    for (auto offset = offsets.begin(); offset != offsets.end(); ++ offset){ //for each occurrence of the source in the sampled sentence pair
        // get source position lowerBound  and  upperBound
        int sourceStart = *offset; // lowerBound is always larger than or equal to 0
        int sourceEnd = sourceStart + sourcePhrase.size() - 1; // upperBound is always larger than or equal to 0, because sourcePhrase.size()>=1

        // find the minimally matching foreign phrase
        int targetStart = targetSentence.size() - 1;
        int targetEnd = -1;

        //for (auto alignPoint = corrected_alignment.begin(); alignPoint != corrected_alignment.end(); ++alignPoint) {
        for (auto alignPoint = alignment.begin(); alignPoint != alignment.end(); ++alignPoint) {
            if ((alignPoint->first < sourceSentence.size()) && (alignPoint->second < targetSentence.size())) {
                if ((alignPoint->first >= sourceStart) && (alignPoint->first <= sourceEnd)) {
                    targetStart = std::min((int) alignPoint->second, targetStart);
                    targetEnd = std::max((int) alignPoint->second, targetEnd);
                }
            }
        }

        alignment_t considered_alignment;
        for (auto alignPoint = alignment.begin(); alignPoint != alignment.end(); ++alignPoint) {

            if ((alignPoint->first < sourceSentence.size()) && (alignPoint->second < targetSentence.size())) {
                if ( ((alignPoint->first >= sourceStart) && (alignPoint->first <= sourceEnd)) || ((alignPoint->second >= targetStart) && (alignPoint->second <= targetEnd)) ){
                    considered_alignment.push_back(*alignPoint);
                }
            }
        }
        ExtractPhrasePairs(sourceSentence, targetSentence, considered_alignment, targetAligned, sourceStart, sourceEnd, targetStart, targetEnd, optionsMap); //add all extracted phrase pairs into outOptions
    }
}

void PhraseTable::ExtractPhrasePairs(const std::vector<wid_t> &sourceSentence,
                                     const std::vector<wid_t> &targetSentence,
                                     const alignment_t &alignment,
                                     const std::vector<bool> &targetAligned,
                                     int sourceStart, int sourceEnd, int targetStart, int targetEnd,
                                     OptionsMap_t &optionsMap) {
    if (targetEnd < 0) // 0-based indexing.
        return;

// Check if alignment points are consistent. if yes, copy
    alignment_t currentAlignments;
    for (auto alignPoint = alignment.begin(); alignPoint != alignment.end(); ++alignPoint) {

        //checking whether there are other alignment points outside the current phrase pair; if yes, return doing nothing, because the phrase pair is not valid
        if ( ( (alignPoint->first >= sourceStart) && (alignPoint->first <= sourceEnd) ) &&  ( (alignPoint->second < targetStart) || (alignPoint->second > targetEnd) ) ){
            return;
        }
        if ( ( (alignPoint->second >= targetStart) && (alignPoint->second <= targetEnd) ) && ( (alignPoint->first < sourceStart) || (alignPoint->first > sourceEnd) ) ){
            return;
        }

        currentAlignments.push_back(*alignPoint);
    }


    int ts = targetStart;
    while (true) {
        int te = targetEnd;
        while (true) {
            TranslationOption option(numScoreComponent);
            option.targetPhrase.insert(option.targetPhrase.begin(),
                                       targetSentence.begin() + ts,
                                       targetSentence.begin() + te + 1);

            option.alignment = currentAlignments;

            //re-set the word positions within the phrase pair, regardless the sentence context
            for (auto a = option.alignment.begin(); a != option.alignment.end(); ++a){
                a->first -= sourceStart;
                a->second -= ts;
            }

            auto key = optionsMap.find(option);
            if (key != optionsMap.end()) {
                key->second += 1;
            } else {
                optionsMap.insert(std::pair<TranslationOption, size_t>(option,1));

            }


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
