//
// Created by Davide  Caroselli on 26/11/15.
//

#ifndef MMTDECODERJNI_JNIUTILS_H
#define MMTDECODERJNI_JNIUTILS_H

#include <jni.h>
#include <string>

#define JNILoadClass(jvm, classname) ((jclass) jvm->NewGlobalRef(jvm->FindClass(classname)))

std::string jni_jstrtostr(JNIEnv *jvm, jstring string);
float jni_jfloattofloat(JNIEnv *jvm, jobject value);

jobject jni_arraylist(JNIEnv *jvm, size_t size);
bool jni_arraylist_add(JNIEnv *jvm, jobject list, jobject element);

jobject jni_mapiterator(JNIEnv *jvm, jobject map);
bool jni_maphasnext(JNIEnv *jvm, jobject iterator);
jobject jni_mapnext(JNIEnv *jvm, jobject iterator);
jobject jni_mapgetkey(JNIEnv *jvm, jobject entry);
jobject jni_mapgetvalue(JNIEnv *jvm, jobject entry);

#endif //MMTDECODERJNI_JNIUTILS_H
