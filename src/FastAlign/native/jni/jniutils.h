//
// Created by Davide  Caroselli on 24/06/16.
//

#ifndef JNIFASTALIGN_HANDLE_H
#define JNIFASTALIGN_HANDLE_H

#include <jni.h>
#include <string>

#define JNI_HANDLE_NAME "nativeHandle"

template <typename T> T *jni_gethandle(JNIEnv *env, jobject obj) {
    jclass c = env->GetObjectClass(obj);
    jlong handle = env->GetLongField(obj, env->GetFieldID(c, JNI_HANDLE_NAME, "J"));
    return reinterpret_cast<T *>(handle);
}

inline std::string jni_jstrtostr(JNIEnv *jvm, jstring string) {
    const char *content = jvm->GetStringUTFChars(string, NULL);
    std::string result = content;
    jvm->ReleaseStringUTFChars(string, content);

    return result;
}

#endif //JNIFASTALIGN_HANDLE_H
