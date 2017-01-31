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

class SessionCache;
class Session;

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
