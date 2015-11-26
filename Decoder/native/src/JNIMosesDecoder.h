//
// Created by Davide  Caroselli on 26/11/15.
//

#ifndef MMTDECODERJNI_JNIMOSESDECODER_H
#define MMTDECODERJNI_JNIMOSESDECODER_H

#include <string>

class JNIMosesDecoder {

public:
    JNIMosesDecoder(std::string);
    std::string translate(std::string);
};


#endif //MMTDECODERJNI_JNIMOSESDECODER_H
