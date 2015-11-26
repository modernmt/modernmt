//
// Created by Davide  Caroselli on 26/11/15.
//

#include <eu_modernmt_decoder_moses_MosesDecoder.h>
#include "jni/handle.h"
#include "JNIMosesDecoder.h"
#include "jni/jconv.h"

JNIMosesDecoder *_new_instance(const char *inifile) {
    const char *argv[2] = {"-f", inifile};

    Moses::Parameter params;

    if (!params.LoadParam(2, argv))
        return NULL;

    // initialize all "global" variables, which are stored in StaticData
    // note: this also loads models such as the language model, etc.
    if (!Moses::StaticData::LoadDataStatic(&params, "moses"))
        return NULL;

    return new JNIMosesDecoder(params);
}

JNIEXPORT void JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_init(JNIEnv *env, jobject self, jstring mosesIni) {
    const char *inifile = env->GetStringUTFChars(mosesIni, NULL);
    JNIMosesDecoder *instance = _new_instance(inifile);
    env->ReleaseStringUTFChars(mosesIni, inifile);

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