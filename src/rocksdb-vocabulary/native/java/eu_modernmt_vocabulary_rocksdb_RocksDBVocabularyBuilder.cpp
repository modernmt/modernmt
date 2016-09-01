//
// Created by Davide  Caroselli on 17/08/16.
//

#include "javah/eu_modernmt_vocabulary_rocksdb_RocksDBVocabularyBuilder.h"
#include <vocabulary/PersistentVocabulary.h>

/*
 * Class:     eu_modernmt_vocabulary_rocksdb_RocksDBVocabularyBuilder
 * Method:    flush
 * Signature: ([Ljava/lang/String;[ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_vocabulary_rocksdb_RocksDBVocabularyBuilder_flush(JNIEnv *jvm, jobject jself, jobjectArray jkeys, jintArray jvalues,
                                                    jint nextId, jstring jmodel) {
    const char *model = jvm->GetStringUTFChars(jmodel, NULL);
    PersistentVocabulary vocabulary(model, true);
    jvm->ReleaseStringUTFChars(jmodel, model);

    jsize length = jvm->GetArrayLength(jkeys);
    jint *values = jvm->GetIntArrayElements(jvalues, NULL);

    for (jsize i = 0; i < length; i++) {
        jstring jword = (jstring) jvm->GetObjectArrayElement(jkeys, i);
        const char *word_chars = jvm->GetStringUTFChars(jword, NULL);
        string word = word_chars;
        jvm->ReleaseStringUTFChars(jword, word_chars);

        uint32_t id = (uint32_t) values[i];

        vocabulary.Put(word, id);
    }

    jvm->ReleaseIntArrayElements(jvalues, values, 0);

    vocabulary.ForceCompaction();
    vocabulary.ResetId(nextId);
}