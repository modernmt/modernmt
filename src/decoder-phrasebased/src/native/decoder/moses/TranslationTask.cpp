#include "TranslationTask.h"
#include "StaticData.h"
#include "Sentence.h"
#include "IOWrapper.h"
#include "TranslationAnalysis.h"
#include "TypeDef.h"
#include "Util.h"
#include "Timer.h"
#include "InputType.h"
#include "OutputCollector.h"
#include "mbr.h"

#include "util/exception.hh"

using namespace std;

namespace Moses
{

boost::shared_ptr<TranslationTask>
TranslationTask
::create(boost::shared_ptr<InputType> const& source)
{
  boost::shared_ptr<IOWrapper> nix;
  boost::shared_ptr<TranslationTask> ret(new TranslationTask(source, nix));
  ret->m_self = ret;
  ret->m_scope.reset(new ContextScope);
  return ret;
}

boost::shared_ptr<TranslationTask>
TranslationTask
::create(boost::shared_ptr<InputType> const& source,
         boost::shared_ptr<IOWrapper> const& ioWrapper)
{
  boost::shared_ptr<TranslationTask> ret(new TranslationTask(source, ioWrapper));
  ret->m_self = ret;
  ret->m_scope.reset(new ContextScope);
  return ret;
}

boost::shared_ptr<TranslationTask>
TranslationTask
::create(boost::shared_ptr<InputType> const& source,
         boost::shared_ptr<IOWrapper> const& ioWrapper,
         boost::shared_ptr<ContextScope> const& scope)
{
  boost::shared_ptr<TranslationTask> ret(new TranslationTask(source, ioWrapper));
  ret->m_self  = ret;
  ret->m_scope = scope;
  return ret;
}

TranslationTask
::TranslationTask(boost::shared_ptr<InputType> const& source,
                  boost::shared_ptr<IOWrapper> const& ioWrapper)
  : m_source(source) , m_ioWrapper(ioWrapper)
{
}

TranslationTask::~TranslationTask()
{ }


boost::shared_ptr<BaseManager>
TranslationTask
::SetupManager(SearchAlgorithm algo)
{
  boost::shared_ptr<BaseManager> manager;
  // StaticData const& staticData = StaticData::Instance();
  // if (algo == DefaultSearchAlgorithm) algo = staticData.options().search.algo;

  if (!is_syntax(algo)) {
    manager.reset(new Manager(this->self())); // phrase-based
    return manager;
  }

  UTIL_THROW2("ERROR: requested syntax search algorithm, but compiled with WITHOUT_SYNTAX.");

  return manager;
}

AllOptions::ptr const&
TranslationTask::
options() const
{
  return m_source->options();
}

void TranslationTask::Run()
{
// #ifdef WITH_THREADS
//   s_current.reset(this);
// #endif
  UTIL_THROW_IF2(!m_source || !m_ioWrapper,
                 "Base Instances of TranslationTask must be initialized with"
                 << " input and iowrapper.");

  const size_t translationId = m_source->GetTranslationId();

  // report wall time spent on translation
  Timer translationTime;
  translationTime.start();

  // report thread number
#if defined(WITH_THREADS) && defined(BOOST_HAS_PTHREADS)
  VERBOSE(2, "Translating line " << translationId << "  in thread id "
          << pthread_self() << endl);
#endif


  // execute the translation
  // note: this executes the search, resulting in a search graph
  //       we still need to apply the decision rule (MAP, MBR, ...)
  Timer initTime;
  initTime.start();

  boost::shared_ptr<BaseManager> manager = SetupManager(m_source->options()->search.algo);

  VERBOSE(1, "Line " << translationId << ": Initialize search took "
          << initTime << " seconds total" << endl);

  manager->Decode();

  // new: stop here if m_ioWrapper is NULL. This means that the
  // owner of the TranslationTask will take care of the output
  // oh, and by the way, all the output should be handled by the
  // output wrapper along the lines of *m_iwWrapper << *manager;
  // Just sayin' ...
  if (m_ioWrapper == NULL) return;

  // we are done with search, let's look what we got
  OutputCollector* ocoll;
  Timer additionalReportingTime;
  additionalReportingTime.start();
  boost::shared_ptr<IOWrapper> const& io = m_ioWrapper;

  if(io) {
    // only used in command-line version: moses-main
    manager->OutputBest(io->GetSingleBestOutputCollector());
  }

  // report additional statistics
  manager->CalcDecoderStatistics();
  VERBOSE(1, "Line " << translationId << ": Additional reporting took "
          << additionalReportingTime << " seconds total" << endl);
  VERBOSE(1, "Line " << translationId << ": Translation took "
          << translationTime << " seconds total" << endl);
  IFVERBOSE(2) {
    PrintUserTime("Sentence Decoding Time:");
  }

// #ifdef WITH_THREADS
//   s_current.release();
// #endif
}

}
