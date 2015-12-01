//
// Created by Davide  Caroselli on 26/11/15.
//

#ifndef MMTDECODERJNI_JNIMOSESDECODER_H
#define MMTDECODERJNI_JNIMOSESDECODER_H

#include <string>
#include <moses/server/Server.h>
#include "Feature.h"
#include "Translation.h"

namespace JNIWrapper {
    class MosesDecoder {
        MosesServer::Server m_server;
        xmlrpc_c::methodPtr m_translator;

    public:

        MosesDecoder(Moses::Parameter &);

        std::vector<Feature> getFeatureWeights();

        Translation translate(const std::string &text, uint64_t session,
                              const std::map<std::string, float> *translationContext, size_t nbestListSize);

        int64_t openSession(const std::map<std::string, float> &translationContext);

        void closeSession(uint64_t session);
    };
}

#endif //MMTDECODERJNI_JNIMOSESDECODER_H
