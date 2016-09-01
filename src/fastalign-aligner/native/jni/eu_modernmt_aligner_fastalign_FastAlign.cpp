//
// Created by Davide  Caroselli on 26/08/16.
//

#include "eu_modernmt_aligner_fastalign_FastAlign.h"
#include "jniutils.h"
#include "fastalign/Model.h"
#include <omp.h>

using namespace std;
using namespace fastalign;

inline void ParseSentence(JNIEnv *jvm, jintArray jarray, sentence &outSentence) {
    size_t size = (size_t) jvm->GetArrayLength(jarray);
    outSentence.reserve(size);

    jint *array = jvm->GetIntArrayElements(jarray, NULL);
    for (jsize i = 0; i < size; i++)
        outSentence.push_back((word) array[i]);
    jvm->ReleaseIntArrayElements(jarray, array, 0);
}

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


/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    instantiate
 * Signature: (Ljava/lang/String;I)J
 */
JNIEXPORT jlong JNICALL
Java_eu_modernmt_aligner_fastalign_FastAlign_instantiate(JNIEnv *jvm, jobject jself, jstring jmodel, jint threads) {
    omp_set_dynamic(0);
    omp_set_num_threads(threads);

    string modelPath = jni_jstrtostr(jvm, jmodel);
    return (jlong) Model::Open(modelPath);
}


/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    alignPair
 * Signature: ([I[I)[[I
 */
JNIEXPORT jobjectArray JNICALL
Java_eu_modernmt_aligner_fastalign_FastAlign_alignPair(JNIEnv *jvm, jobject jself, jintArray jsource,
                                                       jintArray jtarget) {
    Model *model = jni_gethandle<Model>(jvm, jself);

    sentence source, target;
    ParseSentence(jvm, jsource, source);
    ParseSentence(jvm, jtarget, target);

    alignment align = model->ComputeAlignment(source, target);

    return AlignmentToArray(jvm, align);
}

/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    alignPairs
 * Signature: ([[I[[I)[[[I
 */
JNIEXPORT jobjectArray JNICALL
Java_eu_modernmt_aligner_fastalign_FastAlign_alignPairs(JNIEnv *jvm, jobject jself, jobjectArray jsources,
                                                        jobjectArray jtargets) {
    Model *model = jni_gethandle<Model>(jvm, jself);
    jsize length = jvm->GetArrayLength(jsources);

    vector<pair<sentence, sentence>> batch;
    batch.reserve((size_t) length);

    for (jsize i = 0; i < length; i++) {
        sentence source, target;
        ParseSentence(jvm, (jintArray) jvm->GetObjectArrayElement(jsources, i), source);
        ParseSentence(jvm, (jintArray) jvm->GetObjectArrayElement(jtargets, i), target);

        batch.push_back(pair<sentence, sentence>(source, target));
    }

    vector<alignment> alignments;
    model->ComputeAlignments(batch, alignments);

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
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_eu_modernmt_aligner_fastalign_FastAlign_dispose(JNIEnv *jvm, jobject jself, jlong ptr) {
    if (ptr != 0) {
        Model *instance = (Model *) ptr;
        delete instance;
    }

    return 0;
}