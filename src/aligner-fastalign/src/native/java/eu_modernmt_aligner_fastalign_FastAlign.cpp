//
// Created by Davide  Caroselli on 26/08/16.
//

#include "javah/eu_modernmt_aligner_fastalign_FastAlign.h"
#include "fastalign/FastAligner.h"
#include "jniutil.h"

#ifdef _OPENMP
#include <omp.h>
#endif


using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

inline void ParseSentence(JNIEnv *jvm, jobjectArray jarray, vector<string> &output) {
    jsize size = jvm->GetArrayLength(jarray);
    output.reserve((size_t) size);

    for (jsize i = 0; i < size; i++) {
        jstring jword = (jstring) jvm->GetObjectArrayElement(jarray, i);
        string word = jni_jstrtostr(jvm, jword);

        output.push_back(word);
    }
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
    string modelPath = jni_jstrtostr(jvm, jmodel);
    return (jlong) new FastAligner(modelPath, threads);
}

/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    align
 * Signature: ([Ljava/lang/String;[Ljava/lang/String;I)[I
 */
JNIEXPORT jintArray JNICALL
Java_eu_modernmt_aligner_fastalign_FastAlign_align___3Ljava_lang_String_2_3Ljava_lang_String_2I
        (JNIEnv *jvm, jobject jself, jobjectArray jsource, jobjectArray jtarget, jint jsym) {
    FastAligner *aligner = jni_gethandle<FastAligner>(jvm, jself);

    vector<string> source, target;
    ParseSentence(jvm, jsource, source);
    ParseSentence(jvm, jtarget, target);

    alignment_t align = aligner->GetAlignment(source, target, (Symmetrization) jsym);
    return AlignmentToArray(jvm, align);
}

/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    align
 * Signature: ([[Ljava/lang/String;[[Ljava/lang/String;[[II)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_aligner_fastalign_FastAlign_align___3_3Ljava_lang_String_2_3_3Ljava_lang_String_2_3_3II
        (JNIEnv *jvm, jobject jself, jobjectArray jsources, jobjectArray jtargets, jobjectArray joutput, jint jsym) {
    FastAligner *aligner = jni_gethandle<FastAligner>(jvm, jself);
    jsize length = jvm->GetArrayLength(jsources);

    vector<pair<vector<string>, vector<string>>> batch;
    batch.reserve((size_t) length);

    for (jsize i = 0; i < length; i++) {
        vector<string> source, target;
        ParseSentence(jvm, (jobjectArray) jvm->GetObjectArrayElement(jsources, i), source);
        ParseSentence(jvm, (jobjectArray) jvm->GetObjectArrayElement(jtargets, i), target);

        batch.push_back(pair<vector<string>, vector<string>>(source, target));
    }

    vector<alignment_t> alignments;
    aligner->GetAlignments(batch, alignments, (Symmetrization) jsym);

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