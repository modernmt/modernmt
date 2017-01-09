// $Id$
#pragma once

#include <boost/thread.hpp>
#include <vector>
#include <map>

#include "SingleFactor.h"
#include "Base.h"

namespace Moses
{

class ScoreComponentCollection;
class Interpolator;

/**
 * A wrapper class for several language models of any type. Enables various interpolation functions.
 *
 * @author David Madl <git@abanbytes.eu>
 */
class LanguageModelMultiplexer : public LanguageModelSingleFactor
{
private:
  typedef ScoreComponentCollection Weights;

protected:
  static LanguageModelMultiplexer* s_instance;

public:
  LanguageModelMultiplexer(const std::string &line, bool registerNow = true);
  ~LanguageModelMultiplexer();

  virtual void SetParameter(const std::string& key, const std::string& value);

  void Load(AllOptions::ptr const& opts);

  //virtual LMResult GetValue(const std::vector<const Word*> &contextFactor, State* finalState = 0) const;

  virtual const FFState* EmptyHypothesisState(const InputType &input) const;

  virtual void EvaluateInIsolation(const Phrase &source
                                   , const TargetPhrase &targetPhrase
                                   , ScoreComponentCollection &scoreBreakdown
                                   , ScoreComponentCollection &estimatedScores) const;

  virtual void CalcScore(const Phrase &phrase, float &fullScore, float &ngramScore, std::size_t &oovCount) const;

  virtual FFState *EvaluateWhenApplied(const Hypothesis &hypo, const FFState *ps, ScoreComponentCollection *out) const;

  virtual FFState* EvaluateWhenApplied(
      const ChartHypothesis& /* cur_hypo */,
      int /* featureID - used to index the state in the previous hypotheses */,
      ScoreComponentCollection* accumulator) const;

  ///////

  /** Called before actual translation of a sentence. Thread-safe. */
  void InitializeForInput(ttasksptr const& ttask);
  /** Called after translation of a sentence. Thread-safe. */
  void CleanUpAfterSentenceProcessing(const InputType& source);

  virtual bool IsUseable(const FactorMask &mask) const;

  // dummy for LanguageModelSingleFactor
  virtual LMResult GetValue(const std::vector<const Word*> &contextFactor, State* finalState = NULL) const;


    static const LanguageModelMultiplexer& Instance() {
      return *s_instance;
    }
    static LanguageModelMultiplexer& InstanceNonConst() {
      return *s_instance;
    }

protected:
  /** Combination function to be performed on the LMs. */
  enum Function {
    INTERPOLATE_LOG_LINEAR,
    INTERPOLATE_LINEAR,
    INTERPOLATE_PLAIN_LINEAR,
    INTERPOLATE_MAX // the maximum of (background score, best domain score)
  };

  Function function_;  ///< combination function to be performed on the LMs
  std::string background_lm_;  ///< feature name of background LM
  float alpha_; ///< adaptive fraction. (1-alpha) * P_background + alpha * P_adaptive, see InitializeForInput()

  boost::thread_specific_ptr<Weights> weights_; ///< feature weights (in active_features_ order), specified dynamically for each sentence
  boost::thread_specific_ptr<std::vector<size_t> > active_features_; ///< active LM indices in features_, specified dynamically for each sentence
  std::vector<LanguageModel *> features_; ///< list of sub-LM FeatureFunctions (including background LM)

  LanguageModel* background_; ///< background LM
  std::vector<LanguageModel *> adaptive_; ///< adaptive LMs

private:
  void initialize_features();

  /** normalize the provided weights map to sum to alpha */
  void normalize_weights(std::map<std::string, float>& map, float alpha = 1.0);

  /** This is thread-specific as it uses a weights_ reference. */
  Interpolator* CreateInterpolator() const;
};


}
