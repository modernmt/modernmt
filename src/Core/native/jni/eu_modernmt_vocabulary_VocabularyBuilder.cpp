//
// Created by Davide  Caroselli on 17/08/16.
//

#include "eu_modernmt_vocabulary_VocabularyBuilder.h"
#include "eu_modernmt_vocabulary_Vocabulary.h"
#include <vocabulary/InMemoryVocabulary.h>
#include "handle.h"

/*
 * Class:     eu_modernmt_vocabulary_VocabularyBuilder
 * Method:    instantiate
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_eu_modernmt_vocabulary_VocabularyBuilder_instantiate(JNIEnv *jvm, jobject jself) {
    InMemoryVocabulary *instance = new InMemoryVocabulary();
    return (jlong) instance;
}

/*
 * Class:     eu_modernmt_vocabulary_VocabularyBuilder
 * Method:    dispose
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_eu_modernmt_vocabulary_VocabularyBuilder_dispose(JNIEnv *jvm, jobject jself, jlong ptr) {
    if (ptr != 0) {
        InMemoryVocabulary *instance = (InMemoryVocabulary *) ptr;
        delete instance;
    }

    return 0;
}

/*
 * Class:     eu_modernmt_vocabulary_VocabularyBuilder
 * Method:    add
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_eu_modernmt_vocabulary_VocabularyBuilder_add(JNIEnv *jvm, jobject jself, jstring jword) {
    return Java_eu_modernmt_vocabulary_Vocabulary_getId(jvm, jself, jword, 1);
}

/*
 * Class:     eu_modernmt_vocabulary_VocabularyBuilder
 * Method:    addLine
 * Signature: ([Ljava/lang/String;)[I
 */
JNIEXPORT jintArray JNICALL
Java_eu_modernmt_vocabulary_VocabularyBuilder_addLine(JNIEnv *jvm, jobject jself, jobjectArray jline) {
    return Java_eu_modernmt_vocabulary_Vocabulary_encodeLine(jvm, jself, jline, 1);
}

/*
 * Class:     eu_modernmt_vocabulary_VocabularyBuilder
 * Method:    addLines
 * Signature: ([[Ljava/lang/String;[[I)V
 */
JNIEXPORT void JNICALL Java_eu_modernmt_vocabulary_VocabularyBuilder_addLines(JNIEnv *jvm, jobject jself, jobjectArray jbuffer, jobjectArray joutput) {
    return Java_eu_modernmt_vocabulary_Vocabulary_encodeLines(jvm, jself, jbuffer, joutput, 1);
}

/*
 * Class:     eu_modernmt_vocabulary_VocabularyBuilder
 * Method:    flush
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_eu_modernmt_vocabulary_VocabularyBuilder_flush(JNIEnv *jvm, jobject jself, jstring jpath) {
    InMemoryVocabulary *self = jni_gethandle<InMemoryVocabulary>(jvm, jself);

    const char *path = jvm->GetStringUTFChars(jpath, NULL);
    self->Flush(path);
    jvm->ReleaseStringUTFChars(jpath, path);
}