//
// Created by Davide  Caroselli on 26/08/16.
//

#include "javah/eu_modernmt_aligner_fastalign_FastAlign.h"
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

inline jintArray AlignmentToArray(JNIEnv *jvm, alignment align) {
    jsize hsize = (jsize) align.size();
    jsize size = (jsize) (hsize * 2);

    jint *buffer = new jint[size];
    for (jsize i = 0; i < hsize; i++) {
        pair<word, word> &pair = align[i];
        buffer[i] = pair.first;
        buffer[i + hsize] = pair.second;
    }

    jintArray jarray = jvm->NewIntArray(size);
    jvm->SetIntArrayRegion(jarray, 0, size, buffer);
    delete[] buffer;

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
 * Method:    align
 * Signature: ([I[I)[I
 */
JNIEXPORT jintArray JNICALL
Java_eu_modernmt_aligner_fastalign_FastAlign_align___3I_3I(JNIEnv *jvm, jobject jself, jintArray jsource,
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
 * Method:    align
 * Signature: ([[I[[I[[I)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_aligner_fastalign_FastAlign_align___3_3I_3_3I_3_3I(JNIEnv *jvm, jobject jself, jobjectArray jsources,
                                                                    jobjectArray jtargets, jobjectArray joutput) {
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

    for (jsize i = 0; i < alignments.size(); i++) {
        jintArray alignment = AlignmentToArray(jvm, alignments[i]);
        jvm->SetObjectArrayElement(joutput, i, alignment);
    }
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