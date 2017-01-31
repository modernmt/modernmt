// -*- c++ -*-
#pragma once

#include <string>
#include <map>
#include <vector>

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
#include "decoder/MosesDecoder.h"

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
  bool m_done;

  translation_request_t const& m_paramList;
  translation_t m_retData;

  Translator* m_translator;
  uint64_t m_session_id;

protected:
  NativeTranslationRequest(translation_request_t const& paramList);

public:

  static
  boost::shared_ptr<NativeTranslationRequest>
  create(Translator* translator,
         translation_request_t const& paramList);


  virtual bool
  DeleteAfterExecution() {
    return false;
  }

  bool
  IsDone() const {
    return m_done;
  }

  translation_t const&
  GetRetData() {
    m_retData.session = m_session_id;
    return m_retData;
  }

  void
  Run();


};

}
