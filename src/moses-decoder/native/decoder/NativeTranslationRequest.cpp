#include "NativeTranslationRequest.h"
#include "ContextScope.h"
#include <boost/foreach.hpp>
#include "Util.h"
#include "Hypothesis.h"
#include "TranslationTask.h"

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
using Moses::TranslationTask;
using Moses::ContextScope;

boost::shared_ptr<NativeTranslationRequest>
NativeTranslationRequest::
create(Translator* translator, translation_request_t const& paramList)
{
  boost::shared_ptr<NativeTranslationRequest> ret;
  ret.reset(new NativeTranslationRequest(paramList));
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


  if(Moses::is_syntax(m_source->options()->search.algo))
    UTIL_THROW2("syntax-based decoding is not supported in MMT decoder");

  run_phrase_decoder();


}

NativeTranslationRequest::
NativeTranslationRequest(translation_request_t const& paramList) :
  m_paramList(paramList)
  , m_session_id(paramList.sessionId)
{
}

void
NativeTranslationRequest::
parse_request()
{
  Session const& S = m_translator->get_session(m_session_id);
  m_scope = S.scope;
  m_session_id = S.id;

  // settings within the *sentence* scope - SetContextWeights() can only override the empty ContextScope if we are not within a session
  if(m_paramList.contextWeights.size() > 0) {
    SPTR<std::map<std::string,float> > M(new std::map<std::string, float>(m_paramList.contextWeights));
    m_scope->SetContextWeights(M);
  }

  boost::shared_ptr<Moses::AllOptions> opts(new Moses::AllOptions());
  *opts = *StaticData::Instance().options();

  if (m_paramList.nBestListSize > 0) {
    opts->nbest.only_distinct = true;
    opts->nbest.nbest_size = m_paramList.nBestListSize;
    opts->nbest.only_distinct = true;
    opts->nbest.enabled = true;
  }

  XVERBOSE(1,"Input: " << m_paramList.sourceSent << endl);

  m_source.reset(new Sentence(opts, 0, m_paramList.sourceSent));
} // end of Translationtask::parse_request()

void
NativeTranslationRequest::
run_phrase_decoder()
{
  Session const& S = m_translator->get_session(m_session_id); // << for 'scope'

  boost::shared_ptr<Moses::InputType> source;
  boost::shared_ptr<Moses::IOWrapper> ioWrapperNone;
  boost::shared_ptr<ContextScope> scope = S.scope;

  Manager manager(TranslationTask::create(source, ioWrapperNone, scope));
  manager.Decode();

  m_retData.text = manager.GetBestTranslation();
  m_retData.alignment = manager.GetWordAlignment();

  if (manager.GetSource().options()->nbest.nbest_size)
    manager.OutputNBest(m_retData.hypotheses);

}
} // namespace MosesServer
