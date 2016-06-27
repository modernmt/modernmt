//
// Created by Davide  Caroselli on 24/06/16.
//

#include "eu_modernmt_aligner_fastalign_FastAlign.h"
#include "handle.h"
#include "jniutil.h"
#include <src/da.h>
#include <sstream>
#include <src/Model.h>

inline jobjectArray AlignmentToArray(JNIEnv *jvm, alignment align) {
    jclass intArrayClass = jvm->FindClass("[I");

    jobjectArray jarray = jvm->NewObjectArray((jsize) align.size(), intArrayClass, NULL);

    for (int i = 0; i < align.size(); i++) {
        std::pair<unsigned, unsigned> pair = align[i];

        jint buffer[2];
        buffer[0] = pair.first;
        buffer[1] = pair.second;

        jintArray jpair = jvm->NewIntArray(2);
        jvm->SetIntArrayRegion(jpair, 0, 2, buffer);

        jvm->SetObjectArrayElement(jarray, i, jpair);
    }

    return jarray;
}

JNIEXPORT void JNICALL Java_eu_modernmt_aligner_fastalign_FastAlign_init(JNIEnv *jvm, jobject jself, jstring jmodelPath,
                                                                         jboolean reverse, jint threads) {
    std::string modelPath = jni_jstrtostr(jvm, jmodelPath);
    Model *model = new Model(modelPath, true, threads, reverse);
    jni_sethandle<Model>(jvm, jself, model);
}

/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    alignPair
 * Signature: (Ljava/lang/String;Ljava/lang/String;)[[I
 */
JNIEXPORT jobjectArray JNICALL Java_eu_modernmt_aligner_fastalign_FastAlign_alignPair(JNIEnv *jvm, jobject jself,
                                                                                      jstring jsource,
                                                                                      jstring jtarget) {
    Model *model = jni_gethandle<Model>(jvm, jself);

    std::string source = jni_jstrtostr(jvm, jsource);
    std::string target = jni_jstrtostr(jvm, jtarget);

    alignment align = model->GetAlignment(source, target);
    return AlignmentToArray(jvm, align);
}

/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    alignPairs
 * Signature: ([Ljava/lang/String;[Ljava/lang/String;)[[[I
 */
JNIEXPORT jobjectArray JNICALL Java_eu_modernmt_aligner_fastalign_FastAlign_alignPairs(JNIEnv *jvm, jobject jself,
                                                                                       jobjectArray jsources,
                                                                                       jobjectArray jtargets) {
    Model *model = jni_gethandle<Model>(jvm, jself);
    jsize length = jvm->GetArrayLength(jsources);

    std::vector<std::pair<std::string, std::string>> pairs;

    for (jsize i = 0; i < length; i++) {
        std::string source = jni_jstrtostr(jvm, (jstring) jvm->GetObjectArrayElement(jsources, i));
        std::string target = jni_jstrtostr(jvm, (jstring) jvm->GetObjectArrayElement(jtargets, i));

        pairs.push_back(std::pair<std::string, std::string>(source, target));
    }

    std::vector<alignment> alignments = model->GetAlignments(pairs);

    jclass intIntArrayClass = jvm->FindClass("[[I");
    jobjectArray jalignments = jvm->NewObjectArray((jsize) alignments.size(), intIntArrayClass, NULL);

    for (jsize i = 0; i < alignments.size(); i++) {
        jobjectArray alignment = AlignmentToArray(jvm, alignments[i]);
        jvm->SetObjectArrayElement(jalignments, i, alignment);
    }

    return jalignments;
}

/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_eu_modernmt_aligner_fastalign_FastAlign_dispos(JNIEnv *jvm, jobject jself) {
    Model *model = jni_gethandle<Model>(jvm, jself);
    delete model;
}