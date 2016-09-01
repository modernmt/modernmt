//
// Created by Davide  Caroselli on 03/12/15.
//

#include "MosesDecoder.h"
#include "JNITranslator.h"
#include <moses/StaticData.h>
#include <moses/FF/StatefulFeatureFunction.h>

using namespace JNIWrapper;

namespace JNIWrapper {

    class MosesDecoderImpl : public MosesDecoder {
        MosesServer::JNITranslator m_translator;
        std::vector<feature_t> m_features;
    public:

        MosesDecoderImpl(Moses::Parameter &param);

        virtual std::vector<feature_t> getFeatures() override;

        virtual std::vector<float> getFeatureWeights(feature_t &feature) override;

        virtual void setDefaultFeatureWeights(const std::map<std::string, std::vector<float>> &featureWeights) override;

        virtual int64_t openSession(const std::map<std::string, float> &translationContext,
                                    const std::map<std::string, std::vector<float>> *featureWeights = NULL) override;

        virtual void closeSession(uint64_t session) override;

        virtual translation_t translate(const std::string &text, uint64_t session,
                                        const std::map<std::string, float> *translationContext,
                                        size_t nbestListSize) override;
    };

}

MosesDecoder *MosesDecoder::createInstance(const char *inifile) {
    const char *argv[2] = {"-f", inifile};

    Moses::Parameter params;

    if (!params.LoadParam(2, argv))
        return NULL;

    // initialize all "global" variables, which are stored in StaticData
    // note: this also loads models such as the language model, etc.
    if (!Moses::StaticData::LoadDataStatic(&params, "moses"))
        return NULL;

    return new MosesDecoderImpl(params);
}

MosesDecoderImpl::MosesDecoderImpl(Moses::Parameter &param) : m_features() {
    const std::vector<const Moses::StatelessFeatureFunction *> &slf = Moses::StatelessFeatureFunction::GetStatelessFeatureFunctions();
    for (size_t i = 0; i < slf.size(); ++i) {
        const Moses::FeatureFunction *feature = slf[i];
        feature_t f;
        f.name = feature->GetScoreProducerDescription();
        f.stateless = feature->IsStateless();
        f.tunable = feature->IsTuneable();
        f.ptr = (void *) feature;

        m_features.push_back(f);
    }

    const std::vector<const Moses::StatefulFeatureFunction *> &sff = Moses::StatefulFeatureFunction::GetStatefulFeatureFunctions();
    for (size_t i = 0; i < sff.size(); ++i) {
        const Moses::FeatureFunction *feature = sff[i];

        feature_t f;
        f.name = feature->GetScoreProducerDescription();
        f.stateless = feature->IsStateless();
        f.tunable = feature->IsTuneable();
        f.ptr = (void *) feature;

        m_features.push_back(f);
    }
}

std::vector<feature_t> MosesDecoderImpl::getFeatures() {
    return m_features;
}

std::vector<float> MosesDecoderImpl::getFeatureWeights(feature_t &_feature) {
    Moses::FeatureFunction *feature = (Moses::FeatureFunction *)_feature.ptr;
    std::vector<float> weights;

    if (feature->IsTuneable()) {
        weights = Moses::StaticData::Instance().GetAllWeightsNew().GetScoresForProducer(feature);

        for (size_t i = 0; i < feature->GetNumScoreComponents(); ++i) {
            if (!feature->IsTuneableComponent(i)) {
                weights[i] = UNTUNEABLE_COMPONENT;
            }
        }
    }

    return weights;
}

void MosesDecoderImpl::setDefaultFeatureWeights(const std::map<std::string, std::vector<float>> &featureWeights) {
    m_translator.set_default_feature_weights(featureWeights);
}

int64_t MosesDecoderImpl::openSession(const std::map<std::string, float> &translationContext, const std::map<std::string, std::vector<float>> *featureWeights) {
    return m_translator.create_session(translationContext, featureWeights);
}

void MosesDecoderImpl::closeSession(uint64_t session) {
    m_translator.delete_session(session);
}

translation_t MosesDecoderImpl::translate(const std::string &text, uint64_t session,
                                          const std::map<std::string, float> *translationContext,
                                          size_t nbestListSize)
{
    // MosesServer interface request...

    MosesServer::TranslationRequest request;
    MosesServer::TranslationResponse response;

    request.sourceSent = text;
    request.nBestListSize = nbestListSize;
    request.sessionId = session;
    if(translationContext != nullptr) {
        assert(session == 0); // setting contextWeights only has an effect if we are not within a session
        request.contextWeights = *translationContext;
    }

    m_translator.execute(request, &response);

    // MosesDecoder JNI interface response.

    // (this split should have allowed us to keep the libjnimoses separate...
    // But the libmoses interface has never really been a stable, separated API anyways
    // [e.g. StaticData leaking into everything],
    // and so libjnimoses always has to be compiled afresh together with moses).

    translation_t translation;

    translation.text = response.text;
    for(auto h: response.hypotheses)
        translation.hypotheses.push_back(hypothesis_t{h.text, h.score, h.fvals});
    translation.session = response.session;
    translation.alignment = response.alignment;

    return translation;
}


