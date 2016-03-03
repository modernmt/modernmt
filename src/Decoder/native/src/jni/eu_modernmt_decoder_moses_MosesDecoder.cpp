//
// Created by Davide  Caroselli on 26/11/15.
//

#include <eu_modernmt_decoder_moses_MosesDecoder.h>
#include <jni/handle.h>
#include <jni/jconv.h>
#include <wrapper/MosesDecoder.h>
#include <stdlib.h>
#include "JMosesFeature.h"
#include "JTranslation.h"

using namespace JNIWrapper;

/*
 * Util function to translate a Java hash map to a std::map
 */
std::map<std::string, float> __parse_context(JNIEnv *jvm, jobject jcontext) {
    std::map<std::string, float> context;

    jobject iterator = jni_mapiterator(jvm, jcontext);

    while (jni_maphasnext(jvm, iterator)) {
        jobject entry = jni_mapnext(jvm, iterator);

        std::string key = jni_jstrtostr(jvm, (jstring) jni_mapgetkey(jvm, entry));
        float value = jni_jfloattofloat(jvm, jni_mapgetvalue(jvm, entry));

        context[key] = value;
    }

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
    JMosesFeature *jMosesFeature = JMosesFeature::instance(jvm);
    MosesDecoder *moses = jni_gethandle<MosesDecoder>(jvm, jself);

    std::vector<feature_t> features = moses->getFeatures();
    jobjectArray array = jvm->NewObjectArray((jsize) features.size(), jMosesFeature->_class, nullptr);

    for (size_t i = 0; i < features.size(); ++i) {
        feature_t feature = features[i];
        jobject jfeature = jMosesFeature->create(jvm, feature.name, feature.tunable, feature.stateless, feature.ptr);
        jvm->SetObjectArrayElement(array, (jsize) i, jfeature);
        jvm->DeleteLocalRef(jfeature);
    }

    return array;
}

/*
 * Class:     eu_modernmt_decoder_moses_MosesDecoder
 * Method:    getFeatureWeights
 * Signature: (Leu/modernmt/decoder/moses/MosesFeature;)[F
 */
JNIEXPORT jfloatArray JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_getFeatureWeights(JNIEnv *jvm, jobject jself,
                                                                                            jobject jfeature) {
    JMosesFeature *jMosesFeature = JMosesFeature::instance(jvm);
    MosesDecoder *moses = jni_gethandle<MosesDecoder>(jvm, jself);

    feature_t mock;
    mock.ptr = jMosesFeature->getPtr(jvm, jfeature);
    std::vector<float> weights = moses->getFeatureWeights(mock);

    jfloat *buffer = (jfloat *) calloc(sizeof(jfloat), weights.size());
    for (size_t i = 0; i < weights.size(); ++i) {
        buffer[i] = weights[i] == MosesDecoder::UNTUNEABLE_COMPONENT ? jMosesFeature->UNTUNEABLE_COMPONENT
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
 * Signature: (Ljava/util/Map;)J
 */
JNIEXPORT jlong JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_createSession(JNIEnv *jvm, jobject self,
                                                                                  jobject translationContext) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);
    std::map<std::string, float> context = __parse_context(jvm, translationContext);
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
 * Signature: (Ljava/lang/String;Ljava/util/Map;JI)Leu/modernmt/decoder/moses/TranslationExchangeObject;
 */
JNIEXPORT jobject JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_translate(JNIEnv *jvm, jobject self, jstring text,
                                                                                jobject translationContext,
                                                                                jlong session, jint nbest) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);
    std::string sentence = jni_jstrtostr(jvm, text);

    translation_t translation;
    if (translationContext != NULL) {
        std::map<std::string, float> context = __parse_context(jvm, translationContext);
        translation = instance->translate(sentence, (uint64_t) session, &context, (size_t) nbest);
    } else {
        translation = instance->translate(sentence, (uint64_t) session, NULL, (size_t) nbest);
    }

    jobjectArray hypothesesArray = NULL;
    std::vector<hypothesis_t> hypotheses = translation.hypotheses;

    if (hypotheses.size() > 0) {
        JHypothesis *jHypothesis = JHypothesis::instance(jvm);
        hypothesesArray = jvm->NewObjectArray((jsize) hypotheses.size(), jHypothesis->_class, nullptr);

        for (size_t i = 0; i < hypotheses.size(); ++i) {
            hypothesis_t hypothesis = hypotheses[i];
            jobject jhypothesis = jHypothesis->create(jvm, hypothesis.text, hypothesis.score, hypothesis.fvals);
            jvm->SetObjectArrayElement(hypothesesArray, (jsize) i, jhypothesis);
            jvm->DeleteLocalRef(jhypothesis);
        }
    }

    JTranslation *jTranslation = JTranslation::instance(jvm);

    jobjectArray jAlignment = jTranslation->getAlignment(jvm, translation.alignment);
    jobject jtranslation = jTranslation->create(jvm, translation.text, hypothesesArray, jAlignment);

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
