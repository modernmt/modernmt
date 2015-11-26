//
// Created by Davide  Caroselli on 26/11/15.
//

#include <eu_modernmt_decoder_moses_MosesDecoder.h>
#include "jni/handle.h"
#include "JNIMosesDecoder.h"
#include "jni/tconv.h"

JNIEXPORT void JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_init(JNIEnv *env, jobject self, jstring _mosesIni) {
    std::string mosesIni;
    jni_jstrtostr(env, _mosesIni, mosesIni);

    JNIMosesDecoder *instance = new JNIMosesDecoder(mosesIni);
    jni_sethandle(env, self, instance);
}

JNIEXPORT jstring JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_translate(JNIEnv *env, jobject self, jstring _text) {
    std::string text;
    jni_jstrtostr(env, _text, text);

    JNIMosesDecoder *instance = jni_gethandle<JNIMosesDecoder>(env, self);
    return env->NewStringUTF(instance->translate(text).c_str());
}

JNIEXPORT void JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_dispose(JNIEnv *env, jobject self) {
    JNIMosesDecoder *instance = jni_gethandle<JNIMosesDecoder>(env, self);
    jni_sethandle<JNIMosesDecoder>(env, self, 0);
    delete instance;
}