// -*- mode: c++; indent-tabs-mode: nil; tab-width:2  -*-
/***********************************************************************
Moses - factored phrase-based language decoder
Copyright (C) 2006 University of Edinburgh

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
***********************************************************************/

#include <limits>
#include <iostream>
#include <memory>
#include <sstream>

#include "FF/FFState.h"
#include "Implementation.h"
#include "TypeDef.h"
#include "Util.h"
#include "Manager.h"
#include "FactorCollection.h"
#include "Phrase.h"
#include "StaticData.h"
#include "util/exception.hh"

using namespace std;

namespace Moses
{
LanguageModelImplementation::LanguageModelImplementation(const std::string &line)
  :LanguageModel(line), m_nGramOrder(0)
{
}

void LanguageModelImplementation::SetParameter(const std::string& key, const std::string& value)
{
  if (key == "order") {
    m_nGramOrder = Scan<size_t>(value);
  } else if (key == "path") {
    m_filePath = value;
  } else {
    LanguageModel::SetParameter(key, value);
  }

}

void LanguageModelImplementation::ShiftOrPush(std::vector<const Word*> &contextFactor, const Word &word) const
{
  if (contextFactor.size() < GetNGramOrder()) {
    contextFactor.push_back(&word);
  } else {
    // shift
    for (size_t currNGramOrder = 0 ; currNGramOrder < GetNGramOrder() - 1 ; currNGramOrder++) {
      contextFactor[currNGramOrder] = contextFactor[currNGramOrder + 1];
    }
    contextFactor[GetNGramOrder() - 1] = &word;
  }
}

LMResult LanguageModelImplementation::GetValueGivenState(
  const std::vector<const Word*> &contextFactor,
  FFState &state) const
{
VERBOSE(2,"LMResult LanguageModelImplementation::GetValueGivenState(const std::vector<const Word*> &contextFactor,...)" << std::endl);
  return GetValueForgotState(contextFactor, state);
}

void LanguageModelImplementation::GetState(
  const std::vector<const Word*> &contextFactor,
  FFState &state) const
{
VERBOSE(2,"LMResult LanguageModelImplementation::GetState(const std::vector<const Word*> &contextFactor,...)" << std::endl);
  GetValueForgotState(contextFactor, state);
}

// Calculate score of a phrase.
void LanguageModelImplementation::CalcScore(const Phrase &phrase, float &fullScore, float &ngramScore, size_t &oovCount) const
{
VERBOSE(2,"LanguageModelImplementation::CalcScore(const Phrase &phrase, float &fullScore,...)" << std::endl);
  fullScore  = 0;
  ngramScore = 0;

  oovCount = 0;

  size_t phraseSize = phrase.GetSize();
  if (!phraseSize) return;

  vector<const Word*> contextFactor;
  contextFactor.reserve(GetNGramOrder());
  std::unique_ptr<FFState> state(NewState((phrase.GetWord(0) == GetSentenceStartWord()) ?
                                        GetBeginSentenceState() : GetNullContextState()));
  size_t currPos = 0;
  while (currPos < phraseSize) {
    const Word &word = phrase.GetWord(currPos);

    if (word.IsNonTerminal()) {
      // do nothing. reset ngram. needed to score target phrases during pt loading in chart decoding
      if (!contextFactor.empty()) {
        // TODO: state operator= ?
        state.reset(NewState(GetNullContextState()));
        contextFactor.clear();
      }
    } else {
      ShiftOrPush(contextFactor, word);
      UTIL_THROW_IF2(contextFactor.size() > GetNGramOrder(),
                     "Can only calculate LM score of phrases up to the n-gram order");

      if (word == GetSentenceStartWord()) {
        // do nothing, don't include prob for <s> unigram
        if (currPos != 0) {
          UTIL_THROW2("Either your data contains <s> in a position other than the first word or your language model is missing <s>.  Did you build your ARPA using IRSTLM and forget to run add-start-end.sh?");
        }
      } else {
        LMResult result = GetValueGivenState(contextFactor, *state);
        fullScore += result.score;
        if (contextFactor.size() == GetNGramOrder())
          ngramScore += result.score;
        if (result.unknown) ++oovCount;
      }
    }

    currPos++;
  }
}

FFState *LanguageModelImplementation::EvaluateWhenApplied(const Hypothesis &hypo, const FFState *ps, ScoreComponentCollection *out) const
{
VERBOSE(2,"FFState *LanguageModelImplementation::EvaluateWhenApplied(const Hypothesis &hypo, const FFState *ps,...)" << std::endl);
  // In this function, we only compute the LM scores of n-grams that overlap a
  // phrase boundary. Phrase-internal scores are taken directly from the
  // translation option.

  // In the case of unigram language models, there is no overlap, so we don't
  // need to do anything.
  if(GetNGramOrder() <= 1)
    return NULL;

  // Empty phrase added? nothing to be done
  if (hypo.GetCurrTargetLength() == 0)
    return ps ? NewState(ps) : NULL;

  IFVERBOSE(2) {
    hypo.GetManager().GetSentenceStats().StartTimeCalcLM();
  }

  const size_t currEndPos = hypo.GetCurrTargetWordsRange().GetEndPos();
  const size_t startPos = hypo.GetCurrTargetWordsRange().GetStartPos();

  // 1st n-gram
  vector<const Word*> contextFactor(GetNGramOrder());
  size_t index = 0;
  for (int currPos = (int) startPos - (int) GetNGramOrder() + 1 ; currPos <= (int) startPos ; currPos++) {
    if (currPos >= 0)
      contextFactor[index++] = &hypo.GetWord(currPos);
    else {
      contextFactor[index++] = &GetSentenceStartWord();
    }
  }
  FFState *res = NewState(ps);
  float lmScore = ps ? GetValueGivenState(contextFactor, *res).score : GetValueForgotState(contextFactor, *res).score;

  // main loop
  size_t endPos = std::min(startPos + GetNGramOrder() - 2
                           , currEndPos);
  for (size_t currPos = startPos + 1 ; currPos <= endPos ; currPos++) {
    // shift all args down 1 place
    for (size_t i = 0 ; i < GetNGramOrder() - 1 ; i++)
      contextFactor[i] = contextFactor[i + 1];

    // add last factor
    contextFactor.back() = &hypo.GetWord(currPos);

    lmScore += GetValueGivenState(contextFactor, *res).score;
  }

  // end of sentence
  if (hypo.IsSourceCompleted()) {
    const size_t size = hypo.GetSize();
    contextFactor.back() = &GetSentenceEndWord();

    for (size_t i = 0 ; i < GetNGramOrder() - 1 ; i ++) {
      int currPos = (int)(size - GetNGramOrder() + i + 1);
      if (currPos < 0)
        contextFactor[i] = &GetSentenceStartWord();
      else
        contextFactor[i] = &hypo.GetWord((size_t)currPos);
    }
    lmScore += GetValueForgotState(contextFactor, *res).score;
  } else {
    if (endPos < currEndPos) {
      //need to get the LM state (otherwise the last LM state is fine)
      for (size_t currPos = endPos+1; currPos <= currEndPos; currPos++) {
        for (size_t i = 0 ; i < GetNGramOrder() - 1 ; i++)
          contextFactor[i] = contextFactor[i + 1];
        contextFactor.back() = &hypo.GetWord(currPos);
      }
      GetState(contextFactor, *res);
    }
  }
  if (OOVFeatureEnabled()) {
    vector<float> scores(2);
    scores[0] = lmScore;
    scores[1] = 0;
    out->PlusEquals(this, scores);
  } else {
    out->PlusEquals(this, lmScore);
  }

  IFVERBOSE(2) {
    hypo.GetManager().GetSentenceStats().StopTimeCalcLM();
  }
  return res;
}

FFState* LanguageModelImplementation::EvaluateWhenApplied(const ChartHypothesis& hypo, int featureID, ScoreComponentCollection* out) const
{
  UTIL_THROW2("Chart decoding support removed.");
}

void LanguageModelImplementation::updateChartScore(float *prefixScore, float *finalizedScore, float score, size_t wordPos) const
{
  if (wordPos < GetNGramOrder()) {
    *prefixScore += score;
  } else {
    *finalizedScore += score;
  }
}

}
