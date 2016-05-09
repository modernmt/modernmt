//
// Created by Davide  Caroselli on 26/11/15.
//

#include <eu_modernmt_decoder_moses_MosesDecoder.h>
#include <jni/handle.h>
#include <jni/jniutil.h>
#include <wrapper/MosesDecoder.h>
#include <stdlib.h>
#include "JMosesFeature.h"
#include "JTranslation.h"

using namespace JNIWrapper;

/*
 * Util function to translate a Java hash map to a std::map
 */
std::map<std::string, float> __parse_context(JNIEnv *jvm, jobjectArray keys, jfloatArray values) {
    std::map<std::string, float> context;

    int size = jvm->GetArrayLength(values);
    jfloat *valuesArray = jvm->GetFloatArrayElements(values, 0);

    for (int i = 0; i < size; i++) {
        std::string key = jni_jstrtostr(jvm, (jstring) jvm->GetObjectArrayElement(keys, i));
        float value = valuesArray[i];

        context[key] = value;
    }

    jvm->ReleaseFloatArrayElements(values, valuesArray, 0);

    return context;
}

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    init
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_init(JNIEnv *jvm, jobject self, jstring jinifile) {
    const char *inifile = jvm->GetStringUTFChars(jinifile, NULL);
    MosesDecoder *instance = MosesDecoder::createInstance(inifile);
    jvm->ReleaseStringUTFChars(jinifile, inifile);

    jni_sethandle(jvm, self, instance);
}


/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    getFeatures
 * Signature: ()[Leu/modernmt/decoder/moses/MosesFeature;
 */
JNIEXPORT jobjectArray JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_getFeatures(JNIEnv *jvm, jobject jself) {
    MosesDecoder *moses = jni_gethandle<MosesDecoder>(jvm, jself);
    JMosesFeature MosesFeature(jvm);

    std::vector<feature_t> features = moses->getFeatures();
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
JNIEXPORT jfloatArray JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_getFeatureWeightsFromPointer(JNIEnv *jvm, jobject jself, jlong jfeaturePtr) {
    JMosesFeature MosesFeature(jvm);
    MosesDecoder *moses = jni_gethandle<MosesDecoder>(jvm, jself);

    feature_t mock;
    mock.ptr = (void *)jfeaturePtr;
    std::vector<float> weights = moses->getFeatureWeights(mock);

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

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    createSession
 * Signature: ([Ljava/lang/String;[F)J
 */
JNIEXPORT jlong JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_createSession(JNIEnv *jvm, jobject self, jobjectArray contextKeys, jfloatArray contextValues) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);
    std::map<std::string, float> context = __parse_context(jvm, contextKeys, contextValues);
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
 * Signature: (Ljava/lang/String;[Ljava/lang/String;[FJI)Leu/modernmt/decoder/moses/TranslationXObject;
 */
JNIEXPORT jobject JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_translate(JNIEnv *jvm, jobject self, jstring text, jobjectArray contextKeys, jfloatArray contextValues, jlong session, jint nbest) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);
    std::string sentence = jni_jstrtostr(jvm, text);

    translation_t translation;
    if (contextKeys != NULL) {
        std::map<std::string, float> context = __parse_context(jvm, contextKeys, contextValues);
        translation = instance->translate(sentence, (uint64_t) session, &context, (size_t) nbest);
    } else {
        translation = instance->translate(sentence, (uint64_t) session, NULL, (size_t) nbest);
    }

    jobjectArray hypothesesArray = NULL;
    std::vector<hypothesis_t> hypotheses = translation.hypotheses;

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

    jobjectArray jAlignment = Translation.getAlignment(jvm, translation.alignment);
    jobject jtranslation = Translation.create(jvm, translation.text, hypothesesArray, jAlignment);

    jvm->DeleteLocalRef(jAlignment);
    if (hypothesesArray)
        jvm->DeleteLocalRef(hypothesesArray);

    return jtranslation;
}

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_dispose(JNIEnv *jvm, jobject self) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);
    jni_sethandle<MosesDecoder>(jvm, self, 0);
    delete instance;
}
