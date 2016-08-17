//
// Created by Davide  Caroselli on 26/11/15.
//

#ifndef MMTCORE_HANDLE_H
#define MMTCORE_HANDLE_H

#include <jni.h>

#define JNI_HANDLE_NAME "nativeHandle"

template <typename T> T *jni_gethandle(JNIEnv *env, jobject obj) {
    jclass c = env->GetObjectClass(obj);
    jlong handle = env->GetLongField(obj, env->GetFieldID(c, JNI_HANDLE_NAME, "J"));
    return reinterpret_cast<T *>(handle);
}

#endif //MMTCORE_HANDLE_H
