#include "NativeTranslationRequest.h"
#include "moses/ContextScope.h"
#include <boost/foreach.hpp>
#include "moses/Util.h"
#include "moses/Hypothesis.h"

namespace MosesServer
{
using namespace std;
using Moses::Hypothesis;
using Moses::StaticData;
using Moses::Range;
using Moses::ChartHypothesis;
using Moses::Phrase;
using Moses::Manager;
using Moses::SearchGraphNode;
using Moses::TrellisPathList;
using Moses::TranslationOptionCollection;
using Moses::TranslationOptionList;
using Moses::TranslationOption;
using Moses::TargetPhrase;
using Moses::FValue;
using Moses::Sentence;

boost::shared_ptr<NativeTranslationRequest>
NativeTranslationRequest::
create(Translator* translator, TranslationRequest const& paramList,
       boost::condition_variable& cond, boost::mutex& mut)
{
  boost::shared_ptr<NativeTranslationRequest> ret;
  ret.reset(new NativeTranslationRequest(paramList, cond, mut));
  ret->m_self = ret;
  ret->m_translator = translator;
  return ret;
}

void
NativeTranslationRequest::
Run()
{
  parse_request();
  // cerr << "SESSION ID" << ret->m_session_id << endl;


  // settings within the *sentence* scope - SetContextWeights() can only override the empty ContextScope if we are not within a session
  if(m_paramList.contextWeights.size() > 0) {
    SPTR<std::map<std::string,float> > M(new std::map<std::string, float>(m_paramList.contextWeights));
    m_scope->SetContextWeights(M);
  }

  if(Moses::is_syntax(m_options->search.algo))
    run_chart_decoder();
  else
    run_phrase_decoder();

  {
    boost::lock_guard<boost::mutex> lock(m_mutex);
    m_done = true;
  }
  m_cond.notify_one();

}

void
NativeTranslationRequest::
outputNBest(const Manager& manager, std::vector<ResponseHypothesis>& nBest)
{
  TrellisPathList nBestList;

  nBest.clear();

  Moses::NBestOptions const& nbo = m_options->nbest;
  manager.CalcNBest(nbo.nbest_size, nBestList, nbo.only_distinct);
  StaticData const& SD = StaticData::Instance();
  manager.OutputNBest(cout, nBestList);

  //                    m_options->output.factor_order,
  //                    m_source->GetTranslationId(),
  //                    m_options->output.ReportSegmentation);

  BOOST_FOREACH(Moses::TrellisPath const* path, nBestList) {
    vector<const Hypothesis *> const& E = path->GetEdges();
    if (!E.size()) continue;
    std::string target_string;
    pack_hypothesis(manager, E, target_string, NULL);

    // reported in a more structured manner
    ostringstream buf;
    bool with_labels = nbo.include_feature_labels;
    path->GetScoreBreakdown()->OutputAllFeatureScores(buf, with_labels);

    ResponseHypothesis hyp;
    hyp.text = target_string;
    hyp.fvals = buf.str();
    // weighted total score
    hyp.score = path->GetFutureScore();

    nBest.push_back(hyp);
  }
}

void
NativeTranslationRequest::
outputLocalWordAlignment(std::vector<std::pair<size_t, size_t> > &dest, const Moses::Hypothesis *hypo) {
  using namespace std;
  Range const& src = hypo->GetCurrSourceWordsRange();
  Range const& trg = hypo->GetCurrTargetWordsRange();

  Moses::WordAlignmentSort waso = m_options->output.WA_SortOrder;
  vector<pair<size_t,size_t> const* > a
                                      = hypo->GetCurrTargetPhrase().GetAlignTerm().GetSortedAlignments(waso);
  typedef pair<size_t,size_t> item;
  BOOST_FOREACH(item const* p, a) {
    dest.push_back(make_pair(src.GetStartPos() + p->first, trg.GetStartPos() + p->second));
  }
}

NativeTranslationRequest::
NativeTranslationRequest(TranslationRequest const& paramList,
                   boost::condition_variable& cond, boost::mutex& mut)
  : m_cond(cond), m_mutex(mut), m_done(false), m_paramList(paramList)
  , m_session_id(paramList.sessionId)
{
  m_factorOrder.push_back(0);
}

void
NativeTranslationRequest::
parse_request()
{
  Session const& S = m_translator->get_session(m_session_id);
  m_scope = S.scope;
  m_session_id = S.id;

  boost::shared_ptr<Moses::AllOptions> opts(new Moses::AllOptions());
  *opts = *StaticData::Instance().options();

  if (m_paramList.nBestListSize > 0) {
    opts->nbest.only_distinct = true;
    opts->nbest.nbest_size = m_paramList.nBestListSize;
    opts->nbest.only_distinct = true;
    opts->nbest.enabled = true;
  }

  m_options = opts;

  XVERBOSE(1,"Input: " << m_paramList.sourceSent << endl);

  m_source.reset(new Sentence(m_options, 0, m_paramList.sourceSent));
} // end of Translationtask::parse_request()


void
NativeTranslationRequest::
run_chart_decoder()
{
  UTIL_THROW2("NativeTranslationRequest::run_chart_decoder() must not be used anymore.");
}

void
NativeTranslationRequest::
pack_hypothesis(const Moses::Manager& manager, 
		vector<Hypothesis const* > const& edges, std::string& dest, std::vector<std::pair<size_t, size_t> > *alignment)
{
  // target string
  ostringstream target;
  BOOST_REVERSE_FOREACH(Hypothesis const* e, edges) {
    manager.OutputSurface(target, *e); 
  }
  XVERBOSE(1, "BEST TRANSLATION: " << *(manager.GetBestHypothesis()) 
	   << std::endl);

  dest = target.str();

  // word alignment info
  if(alignment != NULL) {
    std::vector <std::pair<size_t, size_t>> &wordAlignment = *alignment;
    BOOST_REVERSE_FOREACH(Hypothesis const*e, edges)
      outputLocalWordAlignment(wordAlignment, e);
  }
}

void
NativeTranslationRequest::
pack_hypothesis(const Moses::Manager& manager, Hypothesis const* h, std::string& dest, std::vector<std::pair<size_t, size_t> > *alignment)
{
  using namespace std;
  vector<Hypothesis const*> edges;
  for (; h; h = h->GetPrevHypo())
    edges.push_back(h);
  pack_hypothesis(manager, edges, dest, alignment);
}


void
NativeTranslationRequest::
run_phrase_decoder()
{
  Manager manager(this->self());
  manager.Decode();
  pack_hypothesis(manager, manager.GetBestHypothesis(), m_retData.text, &m_retData.alignment);

  if (m_options->nbest.nbest_size) outputNBest(manager, m_retData.hypotheses);

}
}