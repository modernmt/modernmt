//
// Created by Davide  Caroselli on 26/11/15.
//

#include "JNIMosesDecoder.h"

JNIMosesDecoder::JNIMosesDecoder(Moses::Parameter params) :
        m_server(MosesServer::Server(params)),
        m_translator(new MosesServer::Translator(m_server)),
        m_close_session(new MosesServer::CloseSession(m_server)) {
}

std::string JNIMosesDecoder::translate(std::string text) {
    std::map<std::string, xmlrpc_c::value> params;
    params["text"] = xmlrpc_c::value_string(text);

    xmlrpc_c::paramList rpcparams;
    rpcparams.add(xmlrpc_c::value_struct(params));
    xmlrpc_c::value retval;
    m_translator->execute(rpcparams, &retval);

    std::map<std::string, xmlrpc_c::value> result = xmlrpc_c::value_struct(retval);

    return xmlrpc_c::value_string(result["text"]);
}
