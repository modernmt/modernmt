//
// Created by Davide  Caroselli on 09/12/15.
//

#include "JTranslation.h"

#define JTranslationClass "eu/modernmt/decoder/phrasebased/TranslationXObject"
#define JHypothesisClass JTranslationClass"$Hypothesis"

JTranslation::JTranslation(JNIEnv *jvm) : _class(jvm->FindClass(JTranslationClass)) {
    constructor = jvm->GetMethodID(_class, "<init>", "(Ljava/lang/String;[L" JHypothesisClass ";[I)V");
}

jobject JTranslation::create(JNIEnv *jvm, std::string &text, jobjectArray nbestList, jintArray alignment) {
    jstring jtext = jvm->NewStringUTF(text.c_str());
    jobject jtranslation = jvm->NewObject(_class, constructor, jtext, nbestList, alignment);
    jvm->DeleteLocalRef(jtext);

    return jtranslation;
}

jintArray JTranslation::getAlignment(JNIEnv *jvm, std::vector<std::pair<size_t, size_t>> alignment) {
    jsize hsize = (jsize) alignment.size();
    jsize size = (jsize) (hsize * 2);

    jint *buffer = new jint[size];
    for (jsize i = 0; i < hsize; i++) {
        std::pair<size_t, size_t> &pair = alignment[i];
        buffer[i] = (jint) pair.first;
        buffer[i + hsize] = (jint) pair.second;
    }

    jintArray jarray = jvm->NewIntArray(size);
    jvm->SetIntArrayRegion(jarray, 0, size, buffer);
    delete[] buffer;

    return jarray;
}



JHypothesis::JHypothesis(JNIEnv *jvm) : _class(jvm->FindClass(JHypothesisClass)) {
    constructor = jvm->GetMethodID(_class, "<init>", "(Ljava/lang/String;FLjava/lang/String;)V");
}

jobject JHypothesis::create(JNIEnv *jvm, std::string &text, float totalScore, std::string &fvals) {
    jstring jtext = jvm->NewStringUTF(text.c_str());
    jstring jfvals = jvm->NewStringUTF(fvals.c_str());
    jobject jhypothesis = jvm->NewObject(_class, constructor, jtext, (jfloat) totalScore, jfvals);
    jvm->DeleteLocalRef(jtext);
    jvm->DeleteLocalRef(jfvals);

    return jhypothesis;
}
