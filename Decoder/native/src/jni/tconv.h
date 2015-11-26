//
// Created by Davide  Caroselli on 26/11/15.
//

#ifndef MMTDECODERJNI_JNIUTILS_H
#define MMTDECODERJNI_JNIUTILS_H

#include <jni.h>
#include <string>

void jni_jstrtostr(JNIEnv *env, jstring jstr, std::string &str) {
    if (!jstr) {
        str.clear();
        return;
    }

    const char *content = env->GetStringUTFChars(jstr, NULL);
    str = content;
    env->ReleaseStringUTFChars(jstr, content);
}

#endif //MMTDECODERJNI_JNIUTILS_H
