/****************************************************
 * Moses - factored phrase-based language decoder   *
 * Copyright (C) 2015 University of Edinburgh       *
 * Licensed under GNU LGPL Version 2.1, see COPYING *
 ****************************************************/

#include "JNITranslator.h"
#include "JNITranslationRequest.h"
//#include "moses/server/Server.h"
#include "moses/ThreadPool.h"

namespace MosesServer
{

using namespace std;
using namespace Moses;

JNITranslator::
JNITranslator(uint32_t numThreads)
    : m_threadPool(new Moses::ThreadPool(numThreads)), m_sessionCache(new SessionCache())
{
}

JNITranslator::
~JNITranslator() {
  delete m_threadPool;
  delete m_sessionCache;
}

Session const&
JNITranslator::
get_session(uint64_t session_id)
{
  return (*m_sessionCache)[session_id];
}

void
JNITranslator::
delete_session(uint64_t const session_id)
{
  return (*m_sessionCache).erase(session_id);
}

void
JNITranslator::
execute(TranslationRequest const& paramList,
        TranslationResponse *   const  retvalP)
{
  boost::condition_variable cond;
  boost::mutex mut;
  boost::shared_ptr<JNITranslationRequest> task;
  task = JNITranslationRequest::create(this, paramList,cond,mut);
  m_threadPool->Submit(task);
  boost::unique_lock<boost::mutex> lock(mut);
  while (!task->IsDone())
    cond.wait(lock);
  *retvalP = task->GetRetData();
}

}
