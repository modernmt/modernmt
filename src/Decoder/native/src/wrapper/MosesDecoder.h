//
// Created by Davide  Caroselli on 03/12/15.
//

#ifndef JNIMOSES_MOSESDECODER_H
#define JNIMOSES_MOSESDECODER_H

#include <stdint.h>
#include <vector>
#include <string>
#include <map>
#include <float.h>

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
} translation_t;

namespace JNIWrapper {
    class MosesDecoder {
    public:
        static constexpr float UNTUNEABLE_COMPONENT = FLT_MAX;

        static MosesDecoder *createInstance(const char *inifile);

        virtual std::vector<feature_t> getFeatures() = 0;

        virtual std::vector<float> getFeatureWeights(feature_t &feature) = 0;

        virtual int64_t openSession(const std::map<std::string, float> &translationContext) = 0;

        virtual void closeSession(uint64_t session) = 0;

        virtual translation_t translate(const std::string &text, uint64_t session,
                                        const std::map<std::string, float> *translationContext,
                                        size_t nbestListSize) = 0;

        virtual ~MosesDecoder() { }
    };
}


#endif //JNIMOSES_MOSESDECODER_H
