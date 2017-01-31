#include "NativeTranslationRequest.h"
#include "ContextScope.h"

namespace MosesServer
{
using namespace std;

boost::shared_ptr<NativeTranslationRequest>
NativeTranslationRequest::
create(MosesServer::Translator* translator, translation_request_t const& paramList)
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

static void Translate(translation_request_t const& request, boost::shared_ptr<Moses::ContextScope> scope, translation_t &result) {
  boost::shared_ptr<Moses::AllOptions> opts(new Moses::AllOptions());
  *opts = *Moses::StaticData::Instance().options();

  if (request.nBestListSize > 0) {
    opts->nbest.only_distinct = true;
    opts->nbest.nbest_size = request.nBestListSize;
    opts->nbest.enabled = true;
  }

  boost::shared_ptr<Moses::InputType> source(new Moses::Sentence(opts, 0, request.sourceSent));
  boost::shared_ptr<Moses::IOWrapper> ioWrapperNone;

  boost::shared_ptr<Moses::TranslationTask> ttask = Moses::TranslationTask::create(source, ioWrapperNone, scope);

  // note: ~Manager() must run while we still own TranslationTask (because it only has a weak_ptr)
  {
    Moses::Manager manager(ttask);
    manager.Decode();

    result.text = manager.GetBestTranslation();
    result.alignment = manager.GetWordAlignment();

    if (manager.GetSource().options()->nbest.nbest_size)
      manager.OutputNBest(result.hypotheses);
  }
}

void
NativeTranslationRequest::
Run()
{
  MosesServer::Session const& S = m_translator->get_session(m_session_id);
  m_scope = S.scope;
  m_session_id = S.id;

  // to do: move this where? probably where the rest of session management is happening. e.g. we create a new session for id=0 every time,
  // so the ContextWeights do not stay around.
  //
  // note: SetContextWeights() can only override the empty ContextScope if we are not within a session
  if(m_paramList.contextWeights.size() > 0) {
    boost::shared_ptr<std::map<std::string,float> > M(new std::map<std::string, float>(m_paramList.contextWeights));
    S.scope->SetContextWeights(M);
  }

  Translate(m_paramList, S.scope, m_retData);
}

} // namespace MosesServer
