//
// Created by Davide  Caroselli on 03/12/15.
//

#ifndef JNIMOSES_MOSESDECODER_H
#define JNIMOSES_MOSESDECODER_H

#include <stdint.h>
#include <vector>
#include <utility>
#include <string>
#include <map>
#include <float.h>
#include <mmt/IncrementalModel.h>
#include <mmt/aligner/Aligner.h>
#include <mmt/vocabulary/Vocabulary.h>

typedef struct {
    bool stateless;
    bool tunable;
    std::string name;
    void *ptr;
} feature_t;

typedef struct {
    std::string text;
    float score;
    std::string fvals;
} hypothesis_t;

typedef struct {
    std::string text;
    int64_t session;
    std::vector<hypothesis_t> hypotheses;
    std::vector<std::pair<size_t, size_t> > alignment;
} translation_t;

typedef struct {
  std::string sourceSent;
  size_t nBestListSize; //< set to 0 if no n-best list requested
} translation_request_t;


namespace mmt {
    namespace decoder {
        class MosesDecoder {
        public:
            static constexpr float UNTUNEABLE_COMPONENT = FLT_MAX;

            static MosesDecoder *createInstance(const char *inifile, Aligner *aligner, Vocabulary *vocabulary);

            virtual std::vector<feature_t> getFeatures() = 0;

            virtual std::vector<float> getFeatureWeights(feature_t &feature) = 0;

            /**
             * Change moses feature weights to the provided featureWeights.
             *
             * Ordering guarantees:
             * * this call will not affect any translations that are in progress.
             * * this call will affect every translation request after its completion.
             *
             * This does not change the 'moses.ini' file itself.
             */
            virtual void setDefaultFeatureWeights(const std::map<std::string, std::vector<float>> &featureWeights) = 0;

            /**
             * Open a new session with the given context weights.
             *
             * @param translationContext  map of context weights
             * @param featureWeights      map of feature weights (may be NULL to use default, global feature weights)
             */
            virtual int64_t openSession(const std::map<std::string, float> &translationContext,
                                        const std::map<std::string, std::vector<float>> *featureWeights = NULL) = 0;

            virtual void closeSession(uint64_t session) = 0;

            /**
             * Translate a sentence.
             *
             * @param text                source sentence with space-separated tokens
             * @param session             either 0 to avoid use of sessions (translate individually), or session ID obtained from openSession()
             * @param translationContext  context weights may be passed here if session == 0
             * @param nbestListSize       if non-zero, produce an n-best list of this size in the translation_t result
             */
            virtual translation_t translate(const std::string &text, uint64_t session,
                                            const std::map<std::string, float> *translationContext,
                                            size_t nbestListSize) = 0;

            /**
             * Returns the list of internal incremental models.
             *
             * @return the list of internal incremental models
             */
            virtual const vector<IncrementalModel *> &GetIncrementalModels() const = 0;

            virtual ~MosesDecoder() {}
        };
    }
}

#endif //JNIMOSES_MOSESDECODER_H
