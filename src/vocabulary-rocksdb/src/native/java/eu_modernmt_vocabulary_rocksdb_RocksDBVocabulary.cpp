//
// Created by Davide  Caroselli on 17/08/16.
//

#include <string>
#include <Vocabulary.h>
#include <mmt/jniutil.h>
#include "javah/eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary.h"

#define LoadStringClass(jvm) (jvm->FindClass("java/lang/String"))

using namespace std;
using namespace mmt;

// Utils

static inline const void ParseStringLine(JNIEnv *jvm, jobjectArray line, vector<string> &output) {
    jsize size = jvm->GetArrayLength(line);
    output.reserve((size_t) size);

    for (jsize i = 0; i < size; i++) {
        jstring jword = (jstring) jvm->GetObjectArrayElement(line, i);
        string word = jni_jstrtostr(jvm, jword);

        output.push_back(word);
    }
}

static inline const jobjectArray EncodeStringLine(JNIEnv *jvm, vector<string> &line, jclass jstringClass) {
    if (jstringClass == NULL)
        jstringClass = LoadStringClass(jvm);

    jsize length = (jsize) line.size();
    jobjectArray result = jvm->NewObjectArray(length, jstringClass, NULL);

    for (jsize i = 0; i < length; ++i) {
        if (!line[i].empty()) {
            jstring jword = jvm->NewStringUTF(line[i].data());
            jvm->SetObjectArrayElement(result, i, jword);
        }
    }

    return result;
}

static inline const void ParseIntLine(JNIEnv *jvm, jintArray jline, vector<wid_t> &output) {
    jsize size = jvm->GetArrayLength(jline);
    output.reserve((size_t) size);

    jint *line = jvm->GetIntArrayElements(jline, NULL);
    for (jsize i = 0; i < size; i++)
        output.push_back((wid_t) line[i]);
    jvm->ReleaseIntArrayElements(jline, line, 0);
}

static inline const jintArray EncodeIntLine(JNIEnv *jvm, vector<wid_t> &line) {
    jsize length = (jsize) line.size();

    const wid_t *rawLine = line.data();
    jint *array = new jint[length];

    for (jsize i = 0; i < length; ++i)
        array[i] = (jint) rawLine[i];

    jintArray result = jvm->NewIntArray(length);
    jvm->SetIntArrayRegion(result, 0, length, array);
    delete[] array;

    return result;
}

/*
 * Class:     eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary
 * Method:    instantiate
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary_instantiate(JNIEnv *jvm, jobject jself, jstring jpath) {
    string path = jni_jstrtostr(jvm, jpath);
    Vocabulary *instance = new Vocabulary(path);

    return (jlong) instance;
}

/*
 * Class:     eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary
 * Method:    dispose
 * Signature: (J)V
 */
JNIEXPORT jlong JNICALL
Java_eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary_dispose(JNIEnv *jvm, jobject jself, jlong ptr) {
    if (ptr != 0) {
        Vocabulary *instance = (Vocabulary *) ptr;
        delete instance;
    }

    return 0;
}

/*
 * Class:     eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary
 * Method:    getId
 * Signature: (Ljava/lang/String;Z)I
 */
JNIEXPORT jint JNICALL
Java_eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary_lookup(JNIEnv *jvm, jobject jself, jstring jword,
                                                             jboolean putIfAbsent) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);
    string word = jni_jstrtostr(jvm, jword);

    wid_t id = self->Lookup(word, putIfAbsent);

    return id;
}

/*
 * Class:     eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary
 * Method:    encodeLine
 * Signature: ([Ljava/lang/String;Z)[I
 */
JNIEXPORT jintArray JNICALL
Java_eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary_lookupLine(JNIEnv *jvm, jobject jself, jobjectArray jline,
                                                                 jboolean putIfAbsent) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);

    vector<string> line;
    ParseStringLine(jvm, jline, line);

    vector<vector<wid_t>> output;
    vector<vector<string>> buffer;
    buffer.push_back(line);

    self->Lookup(buffer, output, putIfAbsent);

    return EncodeIntLine(jvm, output[0]);
}

/*
 * Class:     eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary
 * Method:    encodeLines
 * Signature: ([[Ljava/lang/String;[[IZ)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary_lookupLines(JNIEnv *jvm, jobject jself, jobjectArray jbuffer,
                                                                  jobjectArray joutput, jboolean putIfAbsent) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);


    // Parse input buffer
    jsize bufferSize = jvm->GetArrayLength(jbuffer);
    vector<vector<string>> buffer;

    for (jsize i = 0; i < bufferSize; ++i) {
        jobjectArray jline = (jobjectArray) jvm->GetObjectArrayElement(jbuffer, i);
        vector<string> line;
        ParseStringLine(jvm, jline, line);
        buffer.push_back(line);
    }

    // Lookup
    vector<vector<wid_t>> output;
    self->Lookup(buffer, output, putIfAbsent);

    // Encode result
    for (jsize i = 0; i < bufferSize; ++i)
        jvm->SetObjectArrayElement(joutput, i, EncodeIntLine(jvm, output[i]));
}

/*
 * Class:     eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary
 * Method:    getWord
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary_reverseLookup(JNIEnv *jvm, jobject jself, jint id) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);

    string word;

    if (self->ReverseLookup((wid_t) id, word))
        return word.empty() ? NULL : jvm->NewStringUTF(word.data());
    else
        return NULL;
}

/*
 * Class:     eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary
 * Method:    decodeLine
 * Signature: ([I)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary_reverseLookupLine(JNIEnv *jvm, jobject jself, jintArray jline) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);

    vector<wid_t> line;
    ParseIntLine(jvm, jline, line);

    vector<vector<string>> output;
    vector<vector<wid_t>> buffer;
    buffer.push_back(line);
    self->ReverseLookup(buffer, output);

    return EncodeStringLine(jvm, output[0], NULL);
}

/*
 * Class:     eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary
 * Method:    decodeLines
 * Signature: ([[I[[Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary_reverseLookupLines(JNIEnv *jvm, jobject jself,
                                                                         jobjectArray jbuffer,
                                                                         jobjectArray joutput) {
    Vocabulary *self = jni_gethandle<Vocabulary>(jvm, jself);

    // Parse input buffer
    jsize bufferSize = jvm->GetArrayLength(jbuffer);
    vector<vector<wid_t>> buffer;

    for (jsize i = 0; i < bufferSize; ++i) {
        vector<wid_t> line;
        jintArray jline = (jintArray) jvm->GetObjectArrayElement(jbuffer, i);
        ParseIntLine(jvm, jline, line);
        buffer.push_back(line);
    }

    // Reverse Lookup
    vector<vector<string>> output;
    self->ReverseLookup(buffer, output);

    // Encode result
    jclass jstringClass = LoadStringClass(jvm);
    for (jsize i = 0; i < bufferSize; ++i)
        jvm->SetObjectArrayElement(joutput, i, EncodeStringLine(jvm, output[i], jstringClass));
}