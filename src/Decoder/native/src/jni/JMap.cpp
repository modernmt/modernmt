//
// Created by Davide  Caroselli on 27/04/16.
//

#include "JMap.h"

JMap::JMap(JNIEnv *jvm) : _class(jvm->FindClass("Ljava/util/Map;")),
                          _set_class(jvm->FindClass("Ljava/util/Set;")),
                          _iterator_class(jvm->FindClass("Ljava/util/Iterator;")),
                          _entry_class(jvm->FindClass("Ljava/util/Map$Entry;")) {
    _entrySet = jvm->GetMethodID(_class, "entrySet", "()Ljava/util/Set;");
    _iterator = jvm->GetMethodID(_set_class, "iterator", "()Ljava/util/Iterator;");
    _hasNext = jvm->GetMethodID(_iterator_class, "hasNext", "()Z");
    _next = jvm->GetMethodID(_iterator_class, "next", "()Ljava/lang/Object;");
    _getKey = jvm->GetMethodID(_entry_class, "getKey", "()Ljava/lang/Object;");
    _getValue = jvm->GetMethodID(_entry_class, "getValue", "()Ljava/lang/Object;");
}

JMapIterator JMap::iterator(JNIEnv *jvm, jobject map) {
    jobject entrySet = jvm->CallObjectMethod(map, _entrySet);
    return jvm->CallObjectMethod(entrySet, _iterator);
}

bool JMap::hasNext(JNIEnv *jvm, JMapIterator iterator) {
    return (bool) jvm->CallBooleanMethod(iterator, _hasNext);
}

JMapEntry JMap::next(JNIEnv *jvm, JMapIterator iterator) {
    return jvm->CallObjectMethod(iterator, _next);
}

jobject JMap::getKey(JNIEnv *jvm, JMapEntry entry) {
    return jvm->CallObjectMethod(entry, _getKey);
}

jobject JMap::getValue(JNIEnv *jvm, JMapEntry entry) {
    return jvm->CallObjectMethod(entry, _getValue);
}