// -*- c++ -*-
#pragma once

#include <string>
#include <map>
#include <vector>

#ifdef WITH_THREADS
#include <boost/thread.hpp>
#endif

#include "Util.h"
#include "Hypothesis.h"
#include "Manager.h"
#include "StaticData.h"
#include "ThreadPool.h"
#include "TreeInput.h"
#include "TranslationTask.h"
#include <boost/shared_ptr.hpp>

#include "Session.h"
#include "Translator.h"

namespace MosesServer
{

/**
 * This is a fork of the moses TranslationRequest, which was originally handling XML-RPC
 * calls. Now it is only getting C++ data from JNI (the Java world).
 *
 * @author David Madl (blame me for the duplication)
 * @author Ulrich Germann (original TranslationRequest)
 */
class
NativeTranslationRequest : public virtual Moses::TranslationTask
{
  boost::condition_variable& m_cond;
  boost::mutex& m_mutex;
  bool m_done;

  TranslationRequest const& m_paramList;
  TranslationResponse m_retData;

  Translator* m_translator;
  uint64_t m_session_id;

  std::vector<Moses::FactorType> m_factorOrder;

  void
  parse_request();

  virtual void
  run_phrase_decoder();

protected:
  NativeTranslationRequest(TranslationRequest const& paramList,
                     boost::condition_variable& cond,
                     boost::mutex& mut);

public:

  static
  boost::shared_ptr<NativeTranslationRequest>
  create(Translator* translator,
         TranslationRequest const& paramList,
         boost::condition_variable& cond,
         boost::mutex& mut);


  virtual bool
  DeleteAfterExecution() {
    return false;
  }

  bool
  IsDone() const {
    return m_done;
  }

  TranslationResponse const&
  GetRetData() {
    m_retData.session = m_session_id;
    return m_retData;
  }

  void
  Run();


};

}
