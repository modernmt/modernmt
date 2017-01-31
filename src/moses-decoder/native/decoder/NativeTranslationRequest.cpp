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

NativeTranslationRequest::
NativeTranslationRequest(translation_request_t const& paramList) :
  m_paramList(paramList)
  , m_session_id(paramList.sessionId)
{
}

static void Translate(translation_request_t const& request, boost::shared_ptr<ContextScope> scope, translation_t &result) {

  // TODO move
  // settings within the *sentence* scope - SetContextWeights() can only override the empty ContextScope if we are not within a session
  if(request.contextWeights.size() > 0) {
    boost::shared_ptr<std::map<std::string,float> > M(new std::map<std::string, float>(request.contextWeights));
    scope->SetContextWeights(M);
  }
  // end TODO

  boost::shared_ptr<Moses::AllOptions> opts(new Moses::AllOptions());
  *opts = *StaticData::Instance().options();

  if (request.nBestListSize > 0) {
    opts->nbest.only_distinct = true;
    opts->nbest.nbest_size = request.nBestListSize;
    opts->nbest.enabled = true;
  }

  boost::shared_ptr<Moses::InputType> source(new Sentence(opts, 0, request.sourceSent));
  boost::shared_ptr<Moses::IOWrapper> ioWrapperNone;

  Manager manager(TranslationTask::create(source, ioWrapperNone, scope));
  manager.Decode();

  result.text = manager.GetBestTranslation();
  result.alignment = manager.GetWordAlignment();

  if (manager.GetSource().options()->nbest.nbest_size)
    manager.OutputNBest(result.hypotheses);
}

void
NativeTranslationRequest::
Run()
{
  Session const& S = m_translator->get_session(m_session_id);
  m_scope = S.scope;
  m_session_id = S.id;

  Translate(m_paramList, S.scope, m_retData);
}

} // namespace MosesServer
