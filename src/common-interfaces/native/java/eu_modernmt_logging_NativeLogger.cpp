//
// Created by Davide  Caroselli on 27/12/16.
//

#include <javah/eu_modernmt_logging_NativeLogger.h>
#include <mmt/logging/Logger.h>

using namespace mmt;
using namespace mmt::logging;

JNIEXPORT void JNICALL Java_eu_modernmt_logging_NativeLogger_initialize(JNIEnv *env, jclass jself) {
    Logger::Initialize(env);
}