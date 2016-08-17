//
// Created by Davide  Caroselli on 17/08/16.
//

#include "eu_modernmt_vocabulary_Vocabulary.h"
#include <string>
#include <vocabulary/PersistentVocabulary.h>
#include "handle.h"

#define LoadStringClass(jvm) (jvm->FindClass("java/lang/String"))

using namespace std;

// Utils

static inline const void ParseStringLine(JNIEnv *jvm, jobjectArray line, vector<string> &output) {
    size_t size = (size_t) jvm->GetArrayLength(line);
    output.reserve(size);

    for (jsize i = 0; i < size; i++) {
        jstring jword = (jstring) jvm->GetObjectArrayElement(line, i);
        const char *word_chars = jvm->GetStringUTFChars(jword, NULL);
        string word = word_chars;
        jvm->ReleaseStringUTFChars(jword, word_chars);

        output.push_back(word);
    }
}

static inline const jobjectArray EncodeStringLine(JNIEnv *jvm, vector<string> &line, jclass jstringClass) {
    if (jstringClass == NULL)
        jstringClass = LoadStringClass(jvm);

    jsize length = (jsize) line.size();
    jobjectArray result = jvm->NewObjectArray(length, jstringClass, NULL);

    for (jsize i = 0; i < length; ++i) {
        jstring jword = jvm->NewStringUTF(line[i].data());
        jvm->SetObjectArrayElement(result, i, jword);
    }

    return result;
}

static inline const void ParseIntLine(JNIEnv *jvm, jintArray jline, vector<uint32_t> &output) {
    size_t size = (size_t) jvm->GetArrayLength(jline);
    output.reserve(size);

    jint *line = jvm->GetIntArrayElements(jline, NULL);
    for (jsize i = 0; i < size; i++)
        output.push_back((uint32_t) line[i]);
    jvm->ReleaseIntArrayElements(jline, line, 0);
}

static inline const jintArray EncodeIntLine(JNIEnv *jvm, vector<uint32_t> &line) {
    jsize length = (jsize) line.size();

    const uint32_t *rawLine = line.data();
    jint *array = new jint[length];

    for (jsize i = 0; i < length; ++i)
        array[i] = (jint) rawLine[i];

    jintArray result = jvm->NewIntArray(length);
    jvm->SetIntArrayRegion(result, 0, length, array);
    delete array;

    return result;
}

/*
 * Class:     eu_modernmt_vocabulary_Vocabulary
 * Method:    instantiate
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_eu_modernmt_vocabulary_Vocabulary_instantiate(JNIEnv *jvm, jobject jself, jstring jpath) {
    const char *path = jvm->GetStringUTFChars(jpath, NULL);
    PersistentVocabulary *instance = new PersistentVocabulary(path);
    jvm->ReleaseStringUTFChars(jpath, path);

    return (jlong) instance;
}

/*
 * Class:     eu_modernmt_vocabulary_Vocabulary
 * Method:    dispose
 * Signature: (J)V
 */
JNIEXPORT jlong JNICALL Java_eu_modernmt_vocabulary_Vocabulary_dispose(JNIEnv *jvm, jobject jself, jlong ptr) {
    if (ptr != 0) {
        PersistentVocabulary *instance = (PersistentVocabulary *) ptr;
        delete instance;
    }

    return 0;
}

/*
 * Class:     eu_modernmt_vocabulary_Vocabulary
 * Method:    getId
 * Signature: (Ljava/lang/String;Z)I
 */
JNIEXPORT jint JNICALL
Java_eu_modernmt_vocabulary_Vocabulary_getId(JNIEnv *jvm, jobject jself, jstring jword, jboolean putIfAbsent) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);

    const char *word_chars = jvm->GetStringUTFChars(jword, NULL);
    string word = word_chars;
    uint32_t id = self->Lookup(word, putIfAbsent);
    jvm->ReleaseStringUTFChars(jword, word_chars);

    return id;
}

/*
 * Class:     eu_modernmt_vocabulary_Vocabulary
 * Method:    encodeLine
 * Signature: ([Ljava/lang/String;Z)[I
 */
JNIEXPORT jintArray JNICALL
Java_eu_modernmt_vocabulary_Vocabulary_encodeLine(JNIEnv *jvm, jobject jself, jobjectArray jline,
                                                  jboolean putIfAbsent) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);

    vector<vector<uint32_t>> output;
    vector<vector<string>> buffer(1);
    ParseStringLine(jvm, jline, buffer[1]);

    self->Lookup(buffer, &output, putIfAbsent);

    return EncodeIntLine(jvm, output[1]);
}

/*
 * Class:     eu_modernmt_vocabulary_Vocabulary
 * Method:    encodeLines
 * Signature: ([[Ljava/lang/String;[[IZ)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_vocabulary_Vocabulary_encodeLines(JNIEnv *jvm, jobject jself, jobjectArray jbuffer,
                                                   jobjectArray joutput, jboolean putIfAbsent) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);


    // Parse input buffer
    jsize bufferSize = jvm->GetArrayLength(jbuffer);
    vector<vector<string>> buffer((size_t) bufferSize);

    for (jsize i = 0; i < bufferSize; ++i) {
        jobjectArray jline = (jobjectArray) jvm->GetObjectArrayElement(jbuffer, i);
        ParseStringLine(jvm, jline, buffer[i]);
    }

    // Lookup
    vector<vector<uint32_t>> output;
    self->Lookup(buffer, &output, putIfAbsent);

    // Encode result
    for (jsize i = 0; i < bufferSize; ++i)
        jvm->SetObjectArrayElement(joutput, i, EncodeIntLine(jvm, output[i]));
}

/*
 * Class:     eu_modernmt_vocabulary_Vocabulary
 * Method:    getWord
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_eu_modernmt_vocabulary_Vocabulary_getWord(JNIEnv *jvm, jobject jself, jint id) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);

    string word;

    if (self->ReverseLookup((uint32_t) id, &word))
        return jvm->NewStringUTF(word.data());
    else
        return NULL;
}

/*
 * Class:     eu_modernmt_vocabulary_Vocabulary
 * Method:    decodeLine
 * Signature: ([I)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_eu_modernmt_vocabulary_Vocabulary_decodeLine(JNIEnv *jvm, jobject jself, jintArray jline) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);

    vector<vector<string>> output;
    vector<vector<uint32_t>> buffer(1);
    ParseIntLine(jvm, jline, buffer[1]);

    self->ReverseLookup(buffer, output);

    return EncodeStringLine(jvm, output[1], NULL);
}

/*
 * Class:     eu_modernmt_vocabulary_Vocabulary
 * Method:    decodeLines
 * Signature: ([[I[[Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_vocabulary_Vocabulary_decodeLines(JNIEnv *jvm, jobject jself, jobjectArray jbuffer,
                                                   jobjectArray joutput) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);

    // Parse input buffer
    jsize bufferSize = jvm->GetArrayLength(jbuffer);
    vector<vector<uint32_t>> buffer((size_t) bufferSize);

    for (jsize i = 0; i < bufferSize; ++i) {
        jintArray jline = (jintArray) jvm->GetObjectArrayElement(jbuffer, i);
        ParseIntLine(jvm, jline, buffer[i]);
    }

    // Reverse Lookup
    vector<vector<string>> output;
    self->ReverseLookup(buffer, output);

    // Encode result
    jclass jstringClass = LoadStringClass(jvm);
    for (jsize i = 0; i < bufferSize; ++i)
        jvm->SetObjectArrayElement(joutput, i, EncodeStringLine(jvm, output[i], jstringClass));
}