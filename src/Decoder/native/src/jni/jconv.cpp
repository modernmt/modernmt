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

Map_t GetMap(JNIEnv *jvm) {
    Map_t map;
    map._class = JNILoadClass(jvm, "Ljava/util/Map;");
    map._set_class = JNILoadClass(jvm, "Ljava/util/Set;");
    map._iterator_class = JNILoadClass(jvm, "Ljava/util/Iterator;");
    map._entry_class = JNILoadClass(jvm, "Ljava/util/Map$Entry;");

    map.entrySet = jvm->GetMethodID(map._class, "entrySet", "()Ljava/util/Set;");
    map.iterator = jvm->GetMethodID(map._set_class, "iterator", "()Ljava/util/Iterator;");
    map.hasNext = jvm->GetMethodID(map._iterator_class, "hasNext", "()Z");
    map.next = jvm->GetMethodID(map._iterator_class, "next", "()Ljava/lang/Object;");
    map.getKey = jvm->GetMethodID(map._entry_class, "getKey", "()Ljava/lang/Object;");
    map.getValue = jvm->GetMethodID(map._entry_class, "getValue", "()Ljava/lang/Object;");

    return map;
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