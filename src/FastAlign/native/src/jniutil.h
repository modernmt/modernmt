//
// Created by Davide  Caroselli on 24/06/16.
//

#ifndef JNIFASTALIGN_JNIUTIL_H
#define JNIFASTALIGN_JNIUTIL_H

#include <jni.h>
#include <string>

std::string jni_jstrtostr(JNIEnv *jvm, jstring string) {
    const char *content = jvm->GetStringUTFChars(string, NULL);
    std::string result = content;
    jvm->ReleaseStringUTFChars(string, content);

    return result;
}

#endif //JNIFASTALIGN_JNIUTIL_H
