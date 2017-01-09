// -*- mode: c++; tab-width: 2; indent-tabs-mode: nil -*-
#pragma once

#include <algorithm>

#include <boost/random.hpp>
#include <boost/thread.hpp>
#include <boost/thread/locks.hpp>
#include <boost/intrusive_ptr.hpp>
#include <boost/unordered_set.hpp>
#include <boost/math/distributions/binomial.hpp>

#include "ug_bitext.h"
#include "ug_bitext_pstats.h"
#include "ug_sampling_bias.h"
#include "ug_tsa_array_entry.h"
#include "ug_bitext_phrase_extraction_record.h"
#include "moses/TranslationModel/UG/generic/threading/ug_ref_counter.h"
#include "moses/TranslationModel/UG/generic/threading/ug_thread_safe_counter.h"
#include "moses/TranslationModel/UG/generic/sorting/NBestList.h"

#include "fisheryates.h"

namespace sapt
{

typedef std::vector<id_type> SrcPhrase;
typedef std::size_t size_t;  // make CLion happy

enum 
sampling_method 
  { 
    full_coverage,
    uniform_sampling,
    biased_sampling,
    ranked_sampling, 
    ranked_sampling2,
    ranked_sampling3
  };
  
typedef ttrack::Position TokenPosition;
class CandidateSorter
{
  SamplingBias const& score;
public:
  CandidateSorter(SamplingBias const& s) : score(s) {}
  bool operator()(TokenPosition const& a, TokenPosition const& b) const
  { return score[a.sid] > score[b.sid]; }
};
  
template<typename Token>
class
BitextSampler : public Moses::reference_counter
{
  typedef Bitext<Token> bitext;
  typedef TSA<Token>       tsa;
  typedef SamplingBias    bias_t;
  typedef typename Bitext<Token>::iter tsa_iter;
  mutable boost::condition_variable   m_ready; 
  mutable boost::mutex                 m_lock; 
  // const members
  // SPTR<bitext const> const   m_bitext; // keep bitext alive while I am 
  // should be an 
  SPTR<bitext const> const       m_bitext; // keep bitext alive as long as I am 
  size_t             const         m_plen; // length of lookup phrase
  bool               const          m_fwd; // forward or backward direction?
  SPTR<tsa const>    const         m_root; // root of suffix array
  char               const*        m_next; // current position
  char               const*        m_stop; // end of search range
  sampling_method    const       m_method; // look at all/random/ranked samples 
  SPTR<bias_t const> const         m_bias; // bias over candidates
  size_t             const      m_samples; // how many samples at most 
  size_t             const  m_min_samples;
  // non-const members
  SPTR<pstats>                m_stats; // destination for phrase stats
  size_t                        m_ctr; // number of samples considered
  float                  m_total_bias; // for random sampling with bias
  bool                     m_finished;
  size_t m_num_occurrences; // estimated number of phrase occurrences in corpus
  boost::taus88 m_rnd;  // every job has its own pseudo random generator
  double m_bias_total;

  size_t m_random_size_t;
  double m_rnd_float;

  SrcPhrase m_phrase;

  /**
   * Attempt to extract a phrase pair, and on success, add it to m_stats.
   * @param p: the source phrase (as corpus location + implicit length in m_plen)
   * @ param i1: corpus index 1 to use (may be domain-specific)
   * @ param i2: index 2
   */
  size_t consider_sample(TokenPosition const& p, SPTR<TSA<Token> > i1, SPTR<TSA<Token> > i2);

  size_t consider_sample(TokenPosition const& p);

  size_t perform_ranked_sampling();
  size_t perform_ranked_sampling2();

  size_t perform_ranked_sampling3();
//  size_t ranked3_collect(size_t samples, SPTR<TSA<Token> > i1, SPTR<TSA<Token> > i2);
  size_t uniform_collect(size_t samples, const std::vector<id_type>& domains);


  size_t perform_random_sampling(SamplingBias const* bias);
  size_t perform_full_phrase_extraction();

  int check_sample_distribution(uint64_t const& sid, uint64_t const& offset);
  bool flip_coin(tpt::id_type const& sid, tpt::offset_type const& offset,
                 SamplingBias const* bias);
  bool 
  flip_coin(size_t options_total, 
            size_t const options_considered, 
            size_t const options_chosen,
            size_t const threshold);
  
public:
  BitextSampler(BitextSampler const& other);
  // BitextSampler const& operator=(BitextSampler const& other);
  BitextSampler(SPTR<bitext const> const& bitext,
                SrcPhrase const& phrase,
                bool fwd,
                SPTR<SamplingBias const> const& bias, 
                size_t const min_samples, 
                size_t const max_samples,
                sampling_method const method); 
  ~BitextSampler();
  SPTR<pstats> stats();
  bool done() const;

  bool operator()();

// Ranked sampling sorts all samples by score and then considers the
// top-ranked candidates for phrase extraction.

//namespace bitext {
private:
  bool is_good_sample(TokenPosition const& p);

};

} // end of namespace sapt

