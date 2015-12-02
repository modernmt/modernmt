//
// Created by Davide  Caroselli on 26/11/15.
//

#include "MosesDecoder.h"
#include "moses/FF/StatefulFeatureFunction.h"

using namespace JNIWrapper;

MosesDecoder::MosesDecoder(Moses::Parameter &params) :
        m_server(params),
        m_translator(new MosesServer::Translator(m_server)) {
}

std::vector<Feature> MosesDecoder::getFeatureWeights() {
    const std::vector<const Moses::StatelessFeatureFunction *> &slf = Moses::StatelessFeatureFunction::GetStatelessFeatureFunctions();
    const std::vector<const Moses::StatefulFeatureFunction *> &sff = Moses::StatefulFeatureFunction::GetStatefulFeatureFunctions();

    std::vector<Feature> features;

    size_t index = 0;
    features.resize(sff.size() + slf.size());

    for (size_t i = 0; i < sff.size(); ++i) {
        const Moses::StatefulFeatureFunction *featureFunction = sff[i];
        features[index++].initFromFeature(*featureFunction);
    }

    for (size_t i = 0; i < slf.size(); ++i) {
        const Moses::StatelessFeatureFunction *featureFunction = slf[i];
        features[index++].initFromFeature(*featureFunction);
    }

    return features;
}

Translation MosesDecoder::translate(const std::string &text, uint64_t session,
                                    const std::map<std::string, float> *translationContext, size_t nbestListSize) {
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
    Translation translation;
    std::map<std::string, xmlrpc_c::value>::iterator iterator;

    iterator = result.find("text");
    if (iterator != result.end())
        translation.setText(xmlrpc_c::value_string(iterator->second));

    iterator = result.find("session-id");
    if (iterator != result.end())
        translation.setSession(xmlrpc_c::value_int(iterator->second));

    iterator = result.find("nbest");
    if (iterator != result.end()) {
        std::vector<TranslationHypothesis> &hypotheses = translation.getHypotheses();
        std::vector<xmlrpc_c::value> nbestList = xmlrpc_c::value_array(iterator->second).vectorValueValue();

        for (size_t i = 0; i < nbestList.size(); ++i) {
            std::map<std::string, xmlrpc_c::value> value = xmlrpc_c::value_struct(nbestList[i]);

            TranslationHypothesis hypothesis;
            hypothesis.setText(xmlrpc_c::value_string(value["hyp"]));
            hypothesis.setTotalScore((float) xmlrpc_c::value_double(value["totalScore"]));

            std::string fvals = ((std::string) xmlrpc_c::value_string(value["fvals"])) + " ";
            std::string::size_type offset = 0;
            std::string::size_type limit;

            Feature *feature = nullptr;
            std::vector<Feature> *features = &hypothesis.getScores();

            while ((limit = fvals.find(' ', offset)) != std::string::npos) {
                if (limit > 0 && fvals[limit - 1] != ' ') {
                    if (fvals[limit - 1] == '=') {
                        features->push_back(Feature());
                        feature = &features->at(features->size() - 1);
                        feature->setTuneable(true);
                        feature->setName(fvals.substr(offset, limit - offset - 1));
                    } else if (feature != nullptr) {
                        float score = (float) std::stod(fvals.substr(offset, limit - offset));
                        feature->getWeights().push_back(score);
                    }
                }

                offset = limit + 1;
            }

            hypotheses.push_back(hypothesis);
        }
    }

    return translation;
}

int64_t MosesDecoder::openSession(const std::map<std::string, float> &translationContext) {
    return translate("", 1, &translationContext, 0).getSession();
}

void MosesDecoder::closeSession(uint64_t session) {
    m_server.delete_session(session);
}
