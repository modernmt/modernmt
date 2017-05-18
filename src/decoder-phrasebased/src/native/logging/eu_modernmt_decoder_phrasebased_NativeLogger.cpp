//
// Created by Davide  Caroselli on 27/12/16.
//

#include <eu_modernmt_decoder_phrasebased_NativeLogger.h>
#include "Logger.h"

using namespace mmt;
using namespace mmt::logging;

/*
 * Class:     eu_modernmt_decoder_phrasebased_NativeLogger
 * Method:    initialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_eu_modernmt_decoder_phrasebased_NativeLogger_initialize(JNIEnv *jenv, jclass jself) {
    Logger::Initialize(jenv);
}