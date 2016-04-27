//
// Created by Davide  Caroselli on 27/04/16.
//

#include "JFloat.h"

JFloat::JFloat(JNIEnv *jvm) : _class(jvm->FindClass("Ljava/lang/Float;")) {
    _floatValue = jvm->GetMethodID(_class, "floatValue", "()F");
}

float JFloat::floatValue(JNIEnv *jvm, jobject value) {
    return jvm->CallFloatMethod(value, _floatValue);
}