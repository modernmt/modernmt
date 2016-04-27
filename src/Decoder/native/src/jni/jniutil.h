//
// Created by Davide  Caroselli on 27/04/16.
//

#ifndef JNIMOSES_JNIUTIL_H
#define JNIMOSES_JNIUTIL_H

#include <jni.h>
#include <string>

std::string jni_jstrtostr(JNIEnv *jvm, jstring string) {
    const char *content = jvm->GetStringUTFChars(string, NULL);
    std::string result = content;
    jvm->ReleaseStringUTFChars(string, content);

    return result;
}

#endif //JNIMOSES_JNIUTIL_H
