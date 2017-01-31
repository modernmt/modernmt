/**
 * @file JNITranslator.h: Data structures for JNI interface to moses decoder.
 * Used in MMT project.
 *
 * @author David Madl
 */

#ifndef MMT_SRC_NOSYNC_JNITRANSLATOR_H_H
#define MMT_SRC_NOSYNC_JNITRANSLATOR_H_H

#include <map>
#include <vector>
#include <string>
#include <stdint.h>
#include "decoder/MosesDecoder.h"

namespace Moses {
class ThreadPool;
}

namespace MosesServer {

/**
 * Hypothesis used in n-best list response.
 */
struct ResponseHypothesis {
  std::string text; //< result target sentence
  float score;
  std::string fvals;
};

/**
 * Decoder response to a translation request of a sentence.
 */
struct TranslationResponse {
  std::string text; //< result target sentence
  int64_t session; //< resulting session ID
  std::vector<hypothesis_t> hypotheses; //< n-best list, only filled if requested
  std::vector<std::pair<size_t, size_t> > alignment; //< word alignment using test-trg pairs, e.g. [(0,1), (1,0), ...]
};

/**
 * Translation request of a sentence.
 */
struct TranslationRequest {
  std::string sourceSent;
  size_t nBestListSize; //< set to 0 if no n-best list requested
  uint64_t sessionId; //< 0 means none, 1 means new
  std::map<std::string, float> contextWeights; //< maps from subcorpus name to weight
};

class SessionCache;
class Session;

/**
 * Binary interface to moses decoder. Before usage, load a moses.ini file
 * via Moses::StaticData::LoadDataStatic(), including feature functions.
 */
class
Translator
{
public:
  Translator(uint32_t numThreads = 15);
  ~Translator();

  void execute(translation_request_t const& paramList,
               translation_t *   const  retvalP);

  /** Creates a new moses session. Thread-safe. Destroy them with delete_session() after done. */
  uint64_t create_session(const std::map<std::string, float> &contextWeights, const std::map<std::string, std::vector<float>> *featureWeights = NULL);

  Session const&
  get_session(uint64_t session_id) const;

  void
  delete_session(uint64_t const session_id);

  void
  set_default_feature_weights(const std::map<std::string, std::vector<float>> &featureWeights);

private:
  Moses::ThreadPool* m_threadPool;
  SessionCache* m_sessionCache;
};

}

#endif //MMT_SRC_NOSYNC_JNITRANSLATOR_H_H
