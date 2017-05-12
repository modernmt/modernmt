// -*- mode: c++; indent-tabs-mode: nil; tab-width: 2 -*-
// $Id$

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

#ifndef moses_StaticData_h
#define moses_StaticData_h

#include <stdexcept>
#include <limits>
#include <list>
#include <vector>
#include <map>
#include <memory>
#include <utility>
#include <fstream>
#include <string>

#include <boost/thread.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/locks.hpp>

#include "Parameter.h"
#include "SentenceStats.h"
#include "ScoreComponentCollection.h"

#include "parameters/AllOptions.h"
#include "parameters/BookkeepingOptions.h"

namespace Moses
{

class InputType;
class DecodeGraph;
class DecodeStep;

class DynamicCacheBasedLanguageModel;
class PhraseDictionaryDynamicCacheBased;

class FeatureRegistry;
class PhrasePropertyFactory;

typedef std::pair<std::string, float> UnknownLHSEntry;
typedef std::vector<UnknownLHSEntry>  UnknownLHSList;

/** Contains global variables and contants.
 *  Only 1 object of this class should be instantiated.
 *  A const object of this class is accessible by any function during decoding by calling StaticData::Instance();
 */
class StaticData
{
  friend class HyperParameterAsWeight;

private:
  static StaticData s_instance;
protected:
  Parameter *m_parameter;
  boost::shared_ptr<AllOptions> m_options;

  ScoreComponentCollection m_allWeights;
  mutable boost::mutex m_allWeightsMutex;

  std::vector<DecodeGraph*> m_decodeGraphs;

  // Initial	= 0 = can be used when creating poss trans
  // Other		= 1 = used to calculate LM score once all steps have been processed
  float
  m_wordDeletionWeight;


  // PhraseTrans, Generation & LanguageModelScore has multiple weights.
  // int				m_maxDistortion;
  // do it differently from old pharaoh
  // -ve	= no limit on distortion
  // 0		= no disortion (monotone in old pharaoh)
  bool m_reorderingConstraint; //! use additional reordering constraints
  BookkeepingOptions m_bookkeeping_options;


  bool m_requireSortingAfterSourceContext;

  mutable size_t m_verboseLevel;

  std::string m_factorDelimiter; //! by default, |, but it can be changed


  size_t m_lmcache_cleanup_threshold; //! number of translations after which LM claenup is performed (0=never, N=after N translations; default is 1)

  std::string m_outputUnknownsFile; //! output unknowns in this file

  // Initial = 0 = can be used when creating poss trans
  // Other = 1 = used to calculate LM score once all steps have been processed
  Word m_inputDefaultNonTerminal, m_outputDefaultNonTerminal;
  SourceLabelOverlap m_sourceLabelOverlap;
  UnknownLHSList m_unknownLHS;

  int m_threadCount;
  // long m_startTranslationId;

  bool m_useLegacyPT;
  // bool m_defaultNonTermOnlyForEmptyRange;
  // S2TParsingAlgorithm m_s2tParsingAlgorithm;

  // these are forward declared
  boost::scoped_ptr<FeatureRegistry> m_registry;

  StaticData();

  void LoadChartDecodingParameters();
  void LoadNonTerminals();

  //! load decoding steps
  void LoadDecodeGraphs();
  void LoadDecodeGraphsOld(const std::vector<std::string> &mappingVector,
                           const std::vector<size_t> &maxChartSpans);
  void LoadDecodeGraphsNew(const std::vector<std::string> &mappingVector,
                           const std::vector<size_t> &maxChartSpans);

  void NoCache();

  std::string m_binPath;

  // soft NT lookup for chart models
  std::vector<std::vector<Word> > m_softMatchesMap;

  const StatefulFeatureFunction* m_treeStructure;

  void ini_oov_options();
  bool ini_output_options();
  bool ini_performance_options();

  void initialize_features();
public:

  //! destructor
  ~StaticData();

  //! return static instance for use like global variable
  static const StaticData& Instance() {
    return s_instance;
  }

  //! do NOT call unless you know what you're doing
  static StaticData& InstanceNonConst() {
    return s_instance;
  }

  /** delete current static instance and replace with another.
  	* Used by gui front end
  	*/
#ifdef WIN32
  static void Reset() {
    s_instance = StaticData();
  }
#endif

  //! Load data into static instance. This function is required as
  //  LoadData() is not const
  static bool LoadDataStatic(Parameter *parameter, const std::string &execPath);

  //! Main function to load everything. Also initialize the Parameter object
  bool LoadData(Parameter *parameter);
  void ClearData();

  const Parameter &GetParameter() const {
    return *m_parameter;
  }

  AllOptions::ptr const
    options() const {
    return m_options;
  }

  size_t
  GetVerboseLevel() const {
    return m_verboseLevel;
  }

  void
  SetVerboseLevel(int x) const {
    m_verboseLevel = x;
  }

  /**
   * Get feature weights.
   *
   * This is slow / locking on purpose.
   * You should call ContextScope::GetFeatureWeights() instead!
   */
  ScoreComponentCollection
  GetAllWeightsNew() const { // temporarily called GetAllWeightsNew() so we are aware of all call sites used in MMT.
    boost::lock_guard<boost::mutex> lock(m_allWeightsMutex);
    ScoreComponentCollection copy = m_allWeights;
    return copy;
  }

  ScoreComponentCollection GetAllWeights() const {
    UTIL_THROW2("StaticData::GetAllWeights() should not be called anymore inside MMT - use GetAllWeightsNew() for now.");
    return ScoreComponentCollection();
  }

  /**
   * Change feature weights.
   */
  void SetAllWeights(const ScoreComponentCollection& weights) {
    /*
     * Before decoding each sentence, ContextScope() retrieves the current feature weights from StaticData.
     * Therefore, we can safely lock and change the weights here, which affects all subsequent sentences.
     */
    boost::lock_guard<boost::mutex> lock(m_allWeightsMutex);
    m_allWeights = weights;
  }

  //! DEPRECATED. Use ContextScope::GetFeatureWeights(). Weight for a single-valued feature
  float GetWeight(const FeatureFunction* sp) const {
    boost::lock_guard<boost::mutex> lock(m_allWeightsMutex);
    return m_allWeights.GetScoreForProducer(sp);
  }

  //Weight for a single-valued feature
  void SetWeight(const FeatureFunction* sp, float weight) ;


  //! DEPRECATED. Use ContextScope::GetFeatureWeights(). Weights for feature with fixed number of values
  std::vector<float> GetWeights(const FeatureFunction* sp) const {
    boost::lock_guard<boost::mutex> lock(m_allWeightsMutex);
    return m_allWeights.GetScoresForProducer(sp);
  }

  //Weights for feature with fixed number of values
  void SetWeights(const FeatureFunction* sp, const std::vector<float>& weights);

  const std::string& GetFactorDelimiter() const {
    return m_factorDelimiter;
  }

  size_t GetLMCacheCleanupThreshold() const {
    return m_lmcache_cleanup_threshold;
  }

  const std::string& GetOutputUnknownsFile() const {
    return m_outputUnknownsFile;
  }

  const UnknownLHSList &GetUnknownLHS() const {
    return m_unknownLHS;
  }

  float GetRuleCountThreshold() const {
    return 999999; /* TODO wtf! */
  }

  void ReLoadBleuScoreFeatureParameter(float weight);

  Parameter* GetParameter() {
    return m_parameter;
  }

  int ThreadCount() const {
    return m_threadCount;
  }

  void SetExecPath(const std::string &path);
  const std::string &GetBinDirectory() const;

  bool NeedAlignmentInfo() const {
    return m_bookkeeping_options.need_alignment_info;
  }

  bool IsFeatureFunctionIgnored( const FeatureFunction &ff ) const {
    // a stub left after removing the horror contraption officially dubbed "alternate weight settings"
    return false;
  }

  float GetWeightWordPenalty() const;

  const std::vector<DecodeGraph*>& GetDecodeGraphs() const {
    return m_decodeGraphs;
  }

  //sentence (and thread) specific initialisationn and cleanup
  void InitializeForInput(ttasksptr const& ttask) const;
  void CleanUpAfterSentenceProcessing(ttasksptr const& ttask) const;

  void LoadFeatureFunctions();
  bool CheckWeights() const;
  void LoadSparseWeightsFromConfig();

  std::map<std::string, std::string> OverrideFeatureNames() const;
  void OverrideFeatures();

  const FeatureRegistry &GetFeatureRegistry() const {
    return *m_registry;
  }

  /** check whether we should be using the old code to support binary phrase-table.
  ** eventually, we'll stop support the binary phrase-table and delete this legacy code
  **/
  bool GetUseLegacyPT() const {
    return m_useLegacyPT;
  }

  void SetSoftMatches(std::vector<std::vector<Word> >& softMatchesMap) {
    m_softMatchesMap = softMatchesMap;
  }

  const std::vector< std::vector<Word> >& GetSoftMatches() const {
    return m_softMatchesMap;
  }

  // need global access for output of tree structure
  const StatefulFeatureFunction* GetTreeStructure() const {
    return m_treeStructure;
  }

  void SetTreeStructure(const StatefulFeatureFunction* treeStructure) {
    m_treeStructure = treeStructure;
  }

  bool RequireSortingAfterSourceContext() const {
    return m_requireSortingAfterSourceContext;
  }

};

}
#endif
