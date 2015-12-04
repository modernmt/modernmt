//
// Created by Davide  Caroselli on 03/12/15.
//

#include "MosesDecoder.h"
#include <moses/server/Server.h>
#include <moses/FF/StatefulFeatureFunction.h>

using namespace JNIWrapper;

namespace JNIWrapper {

    class MosesDecoderImpl : public MosesDecoder {
        MosesServer::Server m_server;
        xmlrpc_c::methodPtr m_translator;
        std::vector<feature_t> m_features;
    public:

        MosesDecoderImpl(Moses::Parameter &param);

        virtual std::vector<feature_t> getFeatures() override;

        virtual std::vector<float> getFeatureWeights(feature_t &feature) override;

        virtual int64_t openSession(const std::map<std::string, float> &translationContext) override;

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

MosesDecoderImpl::MosesDecoderImpl(Moses::Parameter &param) : m_server(param),
                                                              m_translator(new MosesServer::Translator(m_server)),
                                                              m_features() {
    const std::vector<const Moses::StatelessFeatureFunction *> &slf = Moses::StatelessFeatureFunction::GetStatelessFeatureFunctions();
    for (size_t i = 0; i < slf.size(); ++i) {
        const Moses::FeatureFunction *feature = slf[i];
        feature_t f = {
                .name = feature->GetScoreProducerDescription(),
                .stateless = feature->IsStateless(),
                .tunable = feature->IsTuneable(),
                .ptr = (void *) feature
        };
        m_features.push_back(f);
    }

    const std::vector<const Moses::StatefulFeatureFunction *> &sff = Moses::StatefulFeatureFunction::GetStatefulFeatureFunctions();
    for (size_t i = 0; i < sff.size(); ++i) {
        const Moses::FeatureFunction *feature = sff[i];
        feature_t f = {
                .name = feature->GetScoreProducerDescription(),
                .stateless = feature->IsStateless(),
                .tunable = feature->IsTuneable(),
                .ptr = (void *) feature
        };
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
        weights = Moses::StaticData::Instance().GetAllWeights().GetScoresForProducer(feature);

        for (size_t i = 0; i < feature->GetNumScoreComponents(); ++i) {
            if (!feature->IsTuneableComponent(i)) {
                weights[i] = UNTUNEABLE_COMPONENT;
            }
        }
    }

    return weights;
}

int64_t MosesDecoderImpl::openSession(const std::map<std::string, float> &translationContext) {
    return translate("", 1, &translationContext, 0).session;
}

void MosesDecoderImpl::closeSession(uint64_t session) {
    m_server.delete_session(session);
}

translation_t MosesDecoderImpl::translate(const std::string &text, uint64_t session,
                                          const std::map<std::string, float> *translationContext,
                                          size_t nbestListSize) {
    // Create request parameters
    std::map<std::string, xmlrpc_c::value> params;

    params["text"] = xmlrpc_c::value_string(text);

    if (session > 0)
        params["session-id"] = xmlrpc_c::value_int((const int) session);

    if (translationContext != nullptr) {
        std::string context;

        for (std::map<std::string, float>::const_iterator iterator = translationContext->begin();
             iterator != translationContext->end(); iterator++) {
            context += (std::string) iterator->first;
            context += ',';
            context += std::to_string(iterator->second);
            context += ':';
        }

        params["context-weights"] = xmlrpc_c::value_string(context.substr(0, context.length() - 1));
    }

    if (nbestListSize > 0) {
        params["add-score-breakdown"] = xmlrpc_c::value_string("true");
        params["nbest-distinct"] = xmlrpc_c::value_string("true");
        params["nbest"] = xmlrpc_c::value_int((const int) nbestListSize);
    }

    // Send request
    xmlrpc_c::paramList rpcparams;
    rpcparams.add(xmlrpc_c::value_struct(params));
    xmlrpc_c::value retval;
    m_translator->execute(rpcparams, &retval);

    std::map<std::string, xmlrpc_c::value> result = xmlrpc_c::value_struct(retval);

    // Parse result
    translation_t translation = {
            .text = std::string(),
            .session = -1,
            .hypotheses = std::vector<hypothesis_t>()
    };

    std::map<std::string, xmlrpc_c::value>::iterator iterator;

    iterator = result.find("text");
    if (iterator != result.end())
        translation.text = xmlrpc_c::value_string(iterator->second);

    iterator = result.find("session-id");
    if (iterator != result.end())
        translation.session = xmlrpc_c::value_int(iterator->second);

    iterator = result.find("nbest");
    if (iterator != result.end()) {
        std::vector<xmlrpc_c::value> nbestList = xmlrpc_c::value_array(iterator->second).vectorValueValue();

        for (size_t i = 0; i < nbestList.size(); ++i) {
            std::map<std::string, xmlrpc_c::value> value = xmlrpc_c::value_struct(nbestList[i]);

            hypothesis_t hypothesis = {
                    .text = xmlrpc_c::value_string(value["hyp"]),
                    .score = (float) xmlrpc_c::value_double(value["totalScore"]),
                    .fvals = (std::string) xmlrpc_c::value_string(value["fvals"])
            };

            translation.hypotheses.push_back(hypothesis);
        }
    }

    return translation;
}


