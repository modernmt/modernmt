//
// Created by Davide  Caroselli on 15/05/17.
//

#ifndef MMT_FASTALIGN_JNIUTIL_H
#define MMT_FASTALIGN_JNIUTIL_H

#include <jni.h>
#include <string>

inline std::string jni_jstrtostr(JNIEnv *jvm, jbyteArray string) {
    signed char *content = jvm->GetByteArrayElements(string, NULL);
    size_t length = (size_t) jvm->GetArrayLength(string);
    std::string result((const char *) content, length);
    jvm->ReleaseByteArrayElements(string, content, JNI_ABORT);

    return result;
}

#endif //MMT_FASTALIGN_JNIUTIL_H
