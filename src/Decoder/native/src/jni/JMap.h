//
// Created by Davide  Caroselli on 27/04/16.
//

#ifndef JNIMOSES_JMAP_H
#define JNIMOSES_JMAP_H

#include <jni.h>

typedef jobject JMapIterator;
typedef jobject JMapEntry;

class JMap {
    const jclass _class;
    const jclass _set_class;
    const jclass _iterator_class;
    const jclass _entry_class;
    jmethodID _entrySet;
    jmethodID _iterator;
    jmethodID _hasNext;
    jmethodID _next;
    jmethodID _getKey;
    jmethodID _getValue;

public:
    JMap(JNIEnv *);

    JMapIterator iterator(JNIEnv *jvm, jobject map);
    bool hasNext(JNIEnv *jvm, JMapIterator iterator);
    JMapEntry next(JNIEnv *jvm, JMapIterator iterator);
    jobject getKey(JNIEnv *jvm, JMapEntry entry);
    jobject getValue(JNIEnv *jvm, JMapEntry entry);
};

#endif //JNIMOSES_JMAP_H
