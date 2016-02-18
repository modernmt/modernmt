//
// Created by Davide  Caroselli on 09/12/15.
//

#include "JTranslation.h"
#include "jconv.h"

#define JTranslationClass "eu/modernmt/decoder/moses/TranslationXObject"
#define JHypothesisClass JTranslationClass"$Hypothesis"

static JTranslation *__JTranslation_instance = NULL;

JTranslation::JTranslation(JNIEnv *jvm) : _class(JNILoadClass(jvm, JTranslationClass)) {
    constructor = jvm->GetMethodID(_class, "<init>", "(Ljava/lang/String;[L" JHypothesisClass ";[[I)V");
}

JTranslation *JTranslation::instance(JNIEnv *jvm) {
    if (__JTranslation_instance == NULL)
        __JTranslation_instance = new JTranslation(jvm);

    return __JTranslation_instance;
}

jobject JTranslation::create(JNIEnv *jvm, std::string &text, jobjectArray nbestList, jobjectArray alignment) {
    jstring jtext = jvm->NewStringUTF(text.c_str());
    jobject jtranslation = jvm->NewObject(_class, constructor, jtext, nbestList, alignment);
    jvm->DeleteLocalRef(jtext);

    return jtranslation;
}

jobjectArray JTranslation::getAlignment(JNIEnv *jvm, std::vector<std::pair<size_t, size_t>> alignment) {
    jclass intArrayClass = jvm->FindClass("[I");
    jobjectArray result = jvm->NewObjectArray(alignment.size(), intArrayClass, NULL);

    for (size_t i = 0; i < alignment.size(); ++i) {
        std::pair<size_t, size_t> pair = alignment[i];

        jintArray jPair = jvm->NewIntArray(2);
        jint jintPair[] = {(jint)pair.first, (jint)pair.second};
        jvm->SetIntArrayRegion(jPair, 0, 2, jintPair);

        jvm->SetObjectArrayElement(result, i, jPair);
        jvm->DeleteLocalRef(jPair);
    }

    return result;
}

static JHypothesis *__JHypothesis_instance = NULL;

JHypothesis::JHypothesis(JNIEnv *jvm) : _class(JNILoadClass(jvm, JHypothesisClass)) {
    constructor = jvm->GetMethodID(_class, "<init>", "(Ljava/lang/String;FLjava/lang/String;)V");
}

JHypothesis *JHypothesis::instance(JNIEnv *jvm) {
    if (__JHypothesis_instance == NULL)
        __JHypothesis_instance = new JHypothesis(jvm);

    return __JHypothesis_instance;
}

jobject JHypothesis::create(JNIEnv *jvm, std::string &text, float totalScore, std::string &fvals) {
    jstring jtext = jvm->NewStringUTF(text.c_str());
    jstring jfvals = jvm->NewStringUTF(fvals.c_str());
    jobject jhypothesis = jvm->NewObject(_class, constructor, jtext, (jfloat) totalScore, jfvals);
    jvm->DeleteLocalRef(jtext);
    jvm->DeleteLocalRef(jfvals);

    return jhypothesis;
}
