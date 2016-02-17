// -*- c++ -*-
#pragma once

#include <string>
#include <map>
#include <vector>

#ifdef WITH_THREADS
#include <boost/thread.hpp>
#endif

#include "moses/Util.h"
#include "moses/ChartManager.h"
#include "moses/Hypothesis.h"
#include "moses/Manager.h"
#include "moses/StaticData.h"
#include "moses/ThreadPool.h"
#include "moses/TranslationModel/PhraseDictionaryMultiModel.h"
#include "moses/TreeInput.h"
#include "moses/TranslationTask.h"
#include <boost/shared_ptr.hpp>

#include "moses/server/Translator.h"
#include "JNITranslator.h"

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
JNITranslationRequest : public virtual Moses::TranslationTask
{
  boost::condition_variable& m_cond;
  boost::mutex& m_mutex;
  bool m_done;

  TranslationRequest const& m_paramList;
  TranslationResponse m_retData;

  JNITranslator* m_translator;
  uint64_t m_session_id;

  void
  parse_request();

  virtual void
  run_chart_decoder();

  virtual void
  run_phrase_decoder();

  void
  pack_hypothesis(const Moses::Manager& manager, 
		              std::vector<Moses::Hypothesis const* > const& edges,
                  std::string & dest) const;

  void
  pack_hypothesis(const Moses::Manager& manager, Moses::Hypothesis const* h,
                  std::string & dest) const;

  void
  add_phrase_aln_info(Moses::Hypothesis const& h,
                      std::vector<xmlrpc_c::value>& aInfo) const;

  void
  outputChartHypo(std::ostream& out, const Moses::ChartHypothesis* hypo);

  bool
  compareSearchGraphNode(const Moses::SearchGraphNode& a,
                         const Moses::SearchGraphNode& b);

  void
  outputNBest(Moses::Manager const& manager,
              std::vector<ResponseHypothesis>& nBestListOut);

protected:
  JNITranslationRequest(TranslationRequest const& paramList,
                     boost::condition_variable& cond,
                     boost::mutex& mut);

public:

  static
  boost::shared_ptr<JNITranslationRequest>
  create(JNITranslator* translator,
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
