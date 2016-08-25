//
// Created by Davide  Caroselli on 24/06/16.
//

#ifndef JNIFASTALIGN_HANDLE_H
#define JNIFASTALIGN_HANDLE_H

#include <jni.h>

#define JNI_HANDLE_NAME "nativeHandle"

jfieldID __jni_gethandlef(JNIEnv *env, jobject obj) {
    jclass c = env->GetObjectClass(obj);
    return env->GetFieldID(c, JNI_HANDLE_NAME, "J");
}

template <typename T> T *jni_gethandle(JNIEnv *env, jobject obj) {
    jlong handle = env->GetLongField(obj, __jni_gethandlef(env, obj));
    return reinterpret_cast<T *>(handle);
}

template <typename T> void jni_sethandle(JNIEnv *env, jobject obj, T *t) {
    jlong handle = reinterpret_cast<jlong>(t);
    env->SetLongField(obj, __jni_gethandlef(env, obj), handle);
}

#endif //JNIFASTALIGN_HANDLE_H
