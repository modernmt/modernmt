//
// Created by Davide  Caroselli on 17/08/16.
//

#include "javah/eu_modernmt_vocabulary_rocksdb_RocksDBVocabularyBuilder.h"
#include <string>
#include <vector>
#include <mmt/jniutil.h>
#include <fstream>

using namespace std;

/*
 * Class:     eu_modernmt_vocabulary_rocksdb_RocksDBVocabularyBuilder
 * Method:    flush
 * Signature: ([Ljava/lang/String;[ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_vocabulary_rocksdb_RocksDBVocabularyBuilder_flush(JNIEnv *jvm, jobject jself, jobjectArray jkeys,
                                                                   jintArray jvalues,
                                                                   jint nextId, jstring jmodel) {
    string model = jni_jstrtostr(jvm, jmodel);

    jsize length = jvm->GetArrayLength(jkeys);
    jint *values = jvm->GetIntArrayElements(jvalues, NULL);

    vector<string> terms((size_t) nextId - 1000);

    for (jsize i = 0; i < length; i++) {
        string term = jni_jstrtostr(jvm, (jstring) jvm->GetObjectArrayElement(jkeys, i));
        terms[values[i] - 1000] = term;
    }

    jvm->ReleaseIntArrayElements(jvalues, values, 0);


    int hole_index = 0;
    ofstream output(model);
    for (auto term = terms.begin(); term != terms.end(); ++term) {
        string word = term->empty() ? ("____MISSING_WORD____" + to_string(hole_index++)) : *term;
        output << word << '\n';
    }
}