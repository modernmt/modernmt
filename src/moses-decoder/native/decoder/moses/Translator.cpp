/****************************************************
 * Moses - factored phrase-based language decoder   *
 * Copyright (C) 2015 University of Edinburgh       *
 * Licensed under GNU LGPL Version 2.1, see COPYING *
 ****************************************************/

#include "Translator.h"
#include "NativeTranslationRequest.h"
#include "moses/ThreadPool.h"

namespace MosesServer {

using namespace std;
using namespace Moses;

Translator::
Translator(uint32_t numThreads)
    : m_threadPool(new Moses::ThreadPool(numThreads)), m_sessionCache(new SessionCache()) {

}

Translator::
~Translator() {
  delete m_threadPool;
  delete m_sessionCache;
}

uint64_t
Translator::
create_session(const std::map<std::string, float> &contextWeights,
               const std::map<std::string, std::vector<float> > *featureWeights) {
  // insertion of session ID 1 magically creates a new Session entry - see SessionCache::operator[]() impl
  Session &session = (*m_sessionCache)[1];

  boost::shared_ptr<std::map<std::string, float> > cw(new std::map<std::string, float>(contextWeights));
  session.scope.reset(new ContextScope(StaticData::Instance().GetAllWeightsNew()));
  session.scope->SetContextWeights(cw);

  if (featureWeights != NULL) {
    boost::shared_ptr<std::map<std::string, std::vector<float> > > fw(
        new std::map<std::string, std::vector<float> >(*featureWeights));
    session.scope->SetFeatureWeights(fw);
  }

  return session.id;
}

Session const &
Translator::
get_session(uint64_t session_id) const {
  return m_sessionCache->at((uint32_t) session_id);
}

void
Translator::
delete_session(uint64_t const session_id) {
  return m_sessionCache->erase((uint32_t) session_id);
}

void
Translator::
set_default_feature_weights(const std::map<std::string, std::vector<float>> &featureWeights)
{
  Session &globalSession = (*m_sessionCache)[0];

  StaticData::InstanceNonConst().SetAllWeights(ScoreComponentCollection::FromWeightMap(featureWeights));

  // flush global caches in old ContextScope, set ContextScope with new feature weights
  globalSession.scope.reset(new ContextScope(StaticData::Instance().GetAllWeightsNew()));
}

void
Translator::
execute(TranslationRequest const& paramList,
        TranslationResponse *   const  retvalP)
{
  boost::condition_variable cond;
  boost::mutex mut;
  boost::shared_ptr<NativeTranslationRequest> task;
  bool have_session = (paramList.sessionId != 0);
  TranslationRequest request = paramList;

  // no session? create one on the fly.
  if(!have_session)
    request.sessionId = create_session(request.contextWeights);

  task = NativeTranslationRequest::create(this, request, cond, mut);
  m_threadPool->Submit(task);
  boost::unique_lock<boost::mutex> lock(mut);
  while (!task->IsDone())
    cond.wait(lock);

  *retvalP = task->GetRetData();

  if(!have_session) {
    delete_session(request.sessionId);
    retvalP->session = 0; // pretend that nothing happened
  }
}

}
