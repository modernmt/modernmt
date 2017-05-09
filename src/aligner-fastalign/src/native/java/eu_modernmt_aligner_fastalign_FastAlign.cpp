//
// Created by Davide  Caroselli on 26/08/16.
//

#include "javah/eu_modernmt_aligner_fastalign_FastAlign.h"
#include "fastalign/FastAligner.h"
#include <mmt/jniutil.h>
#ifdef _OPENMP
#include <omp.h>
#endif


using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

inline void ParseSentence(JNIEnv *jvm, jintArray jarray, vector<wid_t> &outSentence) {
    jsize size = jvm->GetArrayLength(jarray);
    outSentence.reserve((size_t) size);

    jint *array = jvm->GetIntArrayElements(jarray, NULL);
    for (jsize i = 0; i < size; i++)
        outSentence.push_back((wid_t) array[i]);
    jvm->ReleaseIntArrayElements(jarray, array, 0);
}

inline jintArray AlignmentToArray(JNIEnv *jvm, alignment_t align) {
    jsize hsize = (jsize) align.size();
    jsize size = (jsize) (hsize * 2);

    jint *buffer = new jint[size];
    for (jsize i = 0; i < hsize; i++) {
        pair<length_t, length_t> &pair = align[i];
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
#ifdef _OPENMP
    omp_set_dynamic(0);
    omp_set_num_threads(threads);
#endif

    string modelPath = jni_jstrtostr(jvm, jmodel);
    return (jlong) new FastAligner(modelPath);
}


/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    align
 * Signature: ([I[II)[I
 */
JNIEXPORT jintArray JNICALL
Java_eu_modernmt_aligner_fastalign_FastAlign_align___3I_3II(JNIEnv *jvm, jobject jself, jintArray jsource,
                                                            jintArray jtarget, jint jstrategy) {
    FastAligner *aligner = jni_gethandle<FastAligner>(jvm, jself);

    vector<wid_t> source, target;
    ParseSentence(jvm, jsource, source);
    ParseSentence(jvm, jtarget, target);

    alignment_t align = aligner->GetAlignment(source, target, (SymmetrizationStrategy) jstrategy);

    return AlignmentToArray(jvm, align);
}

/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    align
 * Signature: ([[I[[I[[II)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_aligner_fastalign_FastAlign_align___3_3I_3_3I_3_3II(JNIEnv *jvm, jobject jself, jobjectArray jsources,
                                                                     jobjectArray jtargets, jobjectArray joutput,
                                                                     jint jstrategy) {
    FastAligner *aligner = jni_gethandle<FastAligner>(jvm, jself);
    jsize length = jvm->GetArrayLength(jsources);

    vector<pair<vector<wid_t>, vector<wid_t>>> batch;
    batch.reserve((size_t) length);

    for (jsize i = 0; i < length; i++) {
        vector<wid_t> source, target;
        ParseSentence(jvm, (jintArray) jvm->GetObjectArrayElement(jsources, i), source);
        ParseSentence(jvm, (jintArray) jvm->GetObjectArrayElement(jtargets, i), target);

        batch.push_back(pair<vector<wid_t>, vector<wid_t>>(source, target));
    }

    vector<alignment_t> alignments;
    aligner->GetAlignments(batch, alignments, (SymmetrizationStrategy) jstrategy);

    for (jsize i = 0; i < ((jsize) alignments.size()); i++) {
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
        FastAligner *instance = (FastAligner *) ptr;
        delete instance;
    }

    return 0;
}