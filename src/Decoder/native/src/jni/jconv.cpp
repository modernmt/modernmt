//
// Created by Davide  Caroselli on 30/11/15.
//

#include "jconv.h"

std::string jni_jstrtostr(JNIEnv *jvm, jstring string) {
    const char *content = jvm->GetStringUTFChars(string, NULL);
    std::string result = content;
    jvm->ReleaseStringUTFChars(string, content);

    return result;
}

float jni_jfloattofloat(JNIEnv *jvm, jobject value) {
    static jclass FloatClass = NULL;
    static jmethodID FloatValue = NULL;

    if (FloatClass == NULL) {
        FloatClass = JNILoadClass(jvm, "Ljava/lang/Float;");
        FloatValue = jvm->GetMethodID(FloatClass, "floatValue", "()F");
    }

    return jvm->CallFloatMethod(value, FloatValue);
}

// Map

typedef struct {
    jclass _class = NULL;
    jclass _set_class = NULL;
    jclass _iterator_class = NULL;
    jclass _entry_class = NULL;
    jmethodID entrySet = NULL;
    jmethodID iterator = NULL;
    jmethodID hasNext = NULL;
    jmethodID next = NULL;
    jmethodID getKey = NULL;
    jmethodID getValue = NULL;
} Map_t;

static Map_t __Map;

Map_t GetMap(JNIEnv *jvm) {
    if (__Map._class == NULL) {
        __Map._class = JNILoadClass(jvm, "Ljava/util/Map;");
        __Map._set_class = JNILoadClass(jvm, "Ljava/util/Set;");
        __Map._iterator_class = JNILoadClass(jvm, "Ljava/util/Iterator;");
        __Map._entry_class = JNILoadClass(jvm, "Ljava/util/Map$Entry;");

        __Map.entrySet = jvm->GetMethodID(__Map._class, "entrySet", "()Ljava/util/Set;");
        __Map.iterator = jvm->GetMethodID(__Map._set_class, "iterator", "()Ljava/util/Iterator;");
        __Map.hasNext = jvm->GetMethodID(__Map._iterator_class, "hasNext", "()Z");
        __Map.next = jvm->GetMethodID(__Map._iterator_class, "next", "()Ljava/lang/Object;");
        __Map.getKey = jvm->GetMethodID(__Map._entry_class, "getKey", "()Ljava/lang/Object;");
        __Map.getValue = jvm->GetMethodID(__Map._entry_class, "getValue", "()Ljava/lang/Object;");
    }

    return __Map;
}

jobject jni_mapiterator(JNIEnv *jvm, jobject map) {
    Map_t Map = GetMap(jvm);
    jobject entrySet = jvm->CallObjectMethod(map, Map.entrySet);
    return jvm->CallObjectMethod(entrySet, Map.iterator);
}

bool jni_maphasnext(JNIEnv *jvm, jobject iterator) {
    Map_t Map = GetMap(jvm);
    return (bool) jvm->CallBooleanMethod(iterator, Map.hasNext);
}

jobject jni_mapnext(JNIEnv *jvm, jobject iterator) {
    Map_t Map = GetMap(jvm);
    return jvm->CallObjectMethod(iterator, Map.next);
}

jobject jni_mapgetkey(JNIEnv *jvm, jobject entry) {
    Map_t Map = GetMap(jvm);
    return jvm->CallObjectMethod(entry, Map.getKey);
}

jobject jni_mapgetvalue(JNIEnv *jvm, jobject entry) {
    Map_t Map = GetMap(jvm);
    return jvm->CallObjectMethod(entry, Map.getValue);
}