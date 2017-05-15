//
// Created by Davide  Caroselli on 15/05/17.
//

#ifndef MMT_FASTALIGN_JNIUTIL_H
#define MMT_FASTALIGN_JNIUTIL_H

#include <jni.h>
#include <string>

#define JNI_DEFAULT_HANDLE_NAME "nativeHandle"

template <typename T> T *jni_gethandle(JNIEnv *env, jobject obj, const char *handle = NULL) {
    jclass c = env->GetObjectClass(obj);
    jlong ptr = env->GetLongField(obj, env->GetFieldID(c, (handle ? handle : JNI_DEFAULT_HANDLE_NAME), "J"));
    return reinterpret_cast<T *>(ptr);
}

inline std::string jni_jstrtostr(JNIEnv *jvm, jstring string) {
    const char *content = jvm->GetStringUTFChars(string, NULL);
    std::string result = content;
    jvm->ReleaseStringUTFChars(string, content);

    return result;
}

#endif //MMT_FASTALIGN_JNIUTIL_H
