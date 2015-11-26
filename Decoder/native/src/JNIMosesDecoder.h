//
// Created by Davide  Caroselli on 26/11/15.
//

#ifndef MMTDECODERJNI_JNIMOSESDECODER_H
#define MMTDECODERJNI_JNIMOSESDECODER_H

#include <string>
#include <moses/server/Server.h>

class JNIMosesDecoder {
    MosesServer::Server m_server;
    xmlrpc_c::methodPtr const m_translator;
    xmlrpc_c::methodPtr const m_close_session;

public:
    JNIMosesDecoder(Moses::Parameter params);
    std::string translate(std::string);
};


#endif //MMTDECODERJNI_JNIMOSESDECODER_H
