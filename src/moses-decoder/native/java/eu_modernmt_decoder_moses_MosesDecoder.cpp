//
// Created by Davide  Caroselli on 26/11/15.
//

#include <javah/eu_modernmt_decoder_moses_MosesDecoder.h>
#include <decoder/MosesDecoder.h>
#include <stdlib.h>
#include "JMosesFeature.h"
#include "JTranslation.h"
#include <mmt/jniutil.h>

using namespace std;
using namespace mmt::decoder;

void ParseContext(JNIEnv *jvm, jintArray keys, jfloatArray values, map<string, float> &outContext) {
    int size = jvm->GetArrayLength(values);

    jint *keysArray = jvm->GetIntArrayElements(keys, 0);
    jfloat *valuesArray = jvm->GetFloatArrayElements(values, 0);

    for (int i = 0; i < size; i++) {
        string key = std::to_string((uint32_t) keysArray[i]);
        float value = valuesArray[i];

        outContext[key] = value;
    }

    jvm->ReleaseIntArrayElements(keys, keysArray, 0);
    jvm->ReleaseFloatArrayElements(values, valuesArray, 0);
}

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    instantiate
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_eu_modernmt_decoder_moses_MosesDecoder_instantiate(JNIEnv *jvm, jobject jself, jstring jinifile) {
    string inifile = jni_jstrtostr(jvm, jinifile);
    MosesDecoder *instance = MosesDecoder::createInstance(inifile.c_str());
    return (jlong) instance;
}

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    getFeatures
 * Signature: ()[Leu/modernmt/decoder/moses/MosesFeature;
 */
JNIEXPORT jobjectArray JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_getFeatures(JNIEnv *jvm, jobject jself) {
    MosesDecoder *moses = jni_gethandle<MosesDecoder>(jvm, jself);
    JMosesFeature MosesFeature(jvm);

    vector<feature_t> features = moses->getFeatures();
    jobjectArray array = jvm->NewObjectArray((jsize) features.size(), MosesFeature._class, nullptr);

    for (size_t i = 0; i < features.size(); ++i) {
        feature_t feature = features[i];
        jobject jfeature = MosesFeature.create(jvm, feature.name, feature.tunable, feature.stateless, feature.ptr);
        jvm->SetObjectArrayElement(array, (jsize) i, jfeature);
        jvm->DeleteLocalRef(jfeature);
    }

    return array;
}


/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    getFeatureWeightsFromPointer
 * Signature: (J)[F
 */
JNIEXPORT jfloatArray JNICALL
Java_eu_modernmt_decoder_moses_MosesDecoder_getFeatureWeightsFromPointer(JNIEnv *jvm, jobject jself,
                                                                         jlong jfeaturePtr) {
    JMosesFeature MosesFeature(jvm);
    MosesDecoder *moses = jni_gethandle<MosesDecoder>(jvm, jself);

    feature_t mock;
    mock.ptr = (void *) jfeaturePtr;
    vector<float> weights = moses->getFeatureWeights(mock);

    jfloat *buffer = (jfloat *) calloc(sizeof(jfloat), weights.size());
    for (size_t i = 0; i < weights.size(); ++i) {
        buffer[i] = weights[i] == MosesDecoder::UNTUNEABLE_COMPONENT ? MosesFeature.UNTUNEABLE_COMPONENT
                                                                     : (jfloat) weights[i];
    }

    jfloatArray array = jvm->NewFloatArray((jsize) weights.size());
    jvm->SetFloatArrayRegion(array, 0, (jsize) weights.size(), buffer);

    free(buffer);

    return array;
}

#include <iostream>

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    setFeatureWeights
 * Signature: ([Ljava/lang/String;[[F)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_decoder_moses_MosesDecoder_setFeatureWeights(JNIEnv *jvm, jobject self, jobjectArray features,
                                                              jobjectArray weights) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);
    map<string, vector<float>> featureWeights;

    int size = jvm->GetArrayLength(features);

    for (int i = 0; i < size; i++) {
        string feature = jni_jstrtostr(jvm, (jstring) jvm->GetObjectArrayElement(features, i));
        jfloatArray jweights = (jfloatArray) jvm->GetObjectArrayElement(weights, i);

        int wsize = jvm->GetArrayLength(jweights);
        jfloat *weightsArray = jvm->GetFloatArrayElements(jweights, 0);

        featureWeights[feature].assign(weightsArray, weightsArray + wsize);

        jvm->ReleaseFloatArrayElements(jweights, weightsArray, 0);
    }

    instance->setDefaultFeatureWeights(featureWeights);
}


/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    createSession
 * Signature: ([I[F)J
 */
JNIEXPORT jlong JNICALL
Java_eu_modernmt_decoder_moses_MosesDecoder_createSession(JNIEnv *jvm, jobject jself, jintArray contextKeys,
                                                          jfloatArray contextValues) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, jself);

    map<string, float> context;
    ParseContext(jvm, contextKeys, contextValues, context);

    return (jlong) instance->openSession(context);
}

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    destroySession
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_destroySession(JNIEnv *jvm, jobject self,
                                                                                  jlong session) {
    jni_gethandle<MosesDecoder>(jvm, self)->closeSession((uint64_t) session);
}

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    translate
 * Signature: (Ljava/lang/String;[I[FJI)Leu/modernmt/decoder/moses/TranslationXObject;
 */
JNIEXPORT jobject JNICALL
Java_eu_modernmt_decoder_moses_MosesDecoder_translate(JNIEnv *jvm, jobject jself, jstring text, jintArray contextKeys,
                                                      jfloatArray contextValues, jlong session, jint nbest) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, jself);
    string sentence = jni_jstrtostr(jvm, text);

    translation_t translation;
    if (contextKeys != NULL) {
        map<string, float> context;
        ParseContext(jvm, contextKeys, contextValues, context);

        translation = instance->translate(sentence, (uint64_t) session, &context, (size_t) nbest);
    } else {
        translation = instance->translate(sentence, (uint64_t) session, NULL, (size_t) nbest);
    }

    jobjectArray hypothesesArray = NULL;
    vector<hypothesis_t> hypotheses = translation.hypotheses;

    if (hypotheses.size() > 0) {
        JHypothesis Hypothesis(jvm);
        hypothesesArray = jvm->NewObjectArray((jsize) hypotheses.size(), Hypothesis._class, nullptr);

        for (size_t i = 0; i < hypotheses.size(); ++i) {
            hypothesis_t hypothesis = hypotheses[i];
            jobject jhypothesis = Hypothesis.create(jvm, hypothesis.text, hypothesis.score, hypothesis.fvals);
            jvm->SetObjectArrayElement(hypothesesArray, (jsize) i, jhypothesis);
            jvm->DeleteLocalRef(jhypothesis);
        }
    }

    JTranslation Translation(jvm);

    jintArray jAlignment = Translation.getAlignment(jvm, translation.alignment);
    jobject jtranslation = Translation.create(jvm, translation.text, hypothesesArray, jAlignment);

    jvm->DeleteLocalRef(jAlignment);
    if (hypothesesArray)
        jvm->DeleteLocalRef(hypothesesArray);

    return jtranslation;
}

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    updateReceived
 * Signature: (IJI[I[I[I)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_decoder_moses_MosesDecoder_updateReceived(JNIEnv *jvm, jobject jself, jint jstreamId,
                                                           jlong jsentenceId, jint jdomain, jintArray jsource,
                                                           jintArray jtarget,
                                                           jintArray jalignment) {
    throw invalid_argument("Not implemented yet");
}

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    getLatestUpdatesIdentifier
 * Signature: ()[J
 */
JNIEXPORT jlongArray JNICALL
Java_eu_modernmt_decoder_moses_MosesDecoder_getLatestUpdatesIdentifier(JNIEnv *jvm, jobject jself) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, jself);

    vector<mmt::updateid_t> ids = instance->GetLatestUpdatesIdentifier();

    vector<jlong> jidsArray(ids.size());
    for (auto id = ids.begin(); id != ids.end(); ++id) {
        mmt::stream_t stream = id->stream_id;

        if (stream >= jidsArray.size())
            jidsArray.resize(stream + 1);

        jidsArray[stream] = (jlong) id->sentence_id;
    }

    jsize size = (jsize) jidsArray.size();

    jlongArray jarray = jvm->NewLongArray(size);
    jvm->SetLongArrayRegion(jarray, 0, size, jidsArray.data());

    return jarray;
}

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    dispose
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_dispose(JNIEnv *jvm, jobject jself, jlong ptr) {
    if (ptr != 0) {
        MosesDecoder *instance = (MosesDecoder *) ptr;
        delete instance;
    }

    return 0;
}