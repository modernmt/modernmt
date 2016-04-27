//
// Created by Davide  Caroselli on 27/04/16.
//

#ifndef JNIMOSES_JFLOAT_H
#define JNIMOSES_JFLOAT_H

#include <jni.h>

class JFloat {
    const jclass _class;
    jmethodID _floatValue;

public:
    JFloat(JNIEnv *);

    float floatValue(JNIEnv *jvm, jobject value);
};

#endif //JNIMOSES_JFLOAT_H
