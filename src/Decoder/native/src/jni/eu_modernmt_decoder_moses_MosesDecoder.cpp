//
// Created by Davide  Caroselli on 26/11/15.
//

#include <eu_modernmt_decoder_moses_MosesDecoder.h>
#include <jni/handle.h>
#include <jni/jconv.h>
#include <wrapper/MosesDecoder.h>
#include <stdlib.h>
#include "JMosesFeature.h"

using namespace JNIWrapper;

std::map<std::string, float> __parse_context(JNIEnv *, jobject);

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

    feature_t mock = {.ptr = jMosesFeature->getPtr(jvm, jfeature)};
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
JNIEXPORT jobject JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_translate
        (JNIEnv *, jobject, jstring, jobject, jlong, jint);

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
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
//static const char *kTranslationHypothesisClassName = "eu/modernmt/decoder/TranslationHypothesis";
//
//static const char *kTranslationClassName = "eu/modernmt/decoder/Translation";
//
//// Utils
//
//
//
//jobject create_MosesFeature(JNIEnv *jvm, Feature &feature) {
//    static jclass MosesFeatureClass = NULL;
//    static jmethodID MosesFeatureConstructor = NULL;
//    static jfloat JUNTUNEABLE = 0.f;
//
//    if (MosesFeatureClass == NULL) {
//        MosesFeatureClass = JNILoadClass(jvm, kMosesFeatureClassName);
//        MosesFeatureConstructor = jvm->GetMethodID(MosesFeatureClass, "<init>", "(Ljava/lang/String;[F)V");
//        JUNTUNEABLE = jvm->GetStaticFloatField(MosesFeatureClass,
//                                               jvm->GetStaticFieldID(MosesFeatureClass, "UNTUNEABLE", "F"));
//    }
//
//    // Create
//    std::vector<float> weights = feature.getWeights();
//
//    jstring jname = jvm->NewStringUTF(feature.getName().c_str());
//    size_t size = weights.size();
//    jfloatArray jweights = NULL;
//
//    if (size > 0) {
//        jfloat *buffer = (jfloat *) calloc(sizeof(jfloat), size);
//
//        for (size_t i = 0; i < size; ++i) {
//            float w = weights[i];
//            buffer[i] = (w == Feature::UNTUNEABLE ? JUNTUNEABLE : (jfloat) w);
//        }
//
//
//        jweights = jvm->NewFloatArray((jsize) size);
//        jvm->SetFloatArrayRegion(jweights, 0, (jsize) size, buffer);
//        free(buffer);
//    }
//
//    jobject jfeature = jvm->NewObject(MosesFeatureClass, MosesFeatureConstructor, jname, jweights);
//
//    jvm->DeleteLocalRef(jname);
//    jvm->DeleteLocalRef(jweights);
//
//    return jfeature;
//}
//
//jobject create_TranslationHypothesis(JNIEnv *jvm, TranslationHypothesis hypothesis) {
//    static jclass TranslationHypothesisClass = NULL;
//    static jmethodID TranslationHypothesisConstructor = NULL;
//
//    if (TranslationHypothesisClass == NULL) {
//        TranslationHypothesisClass = JNILoadClass(jvm, kTranslationHypothesisClassName);
//        TranslationHypothesisConstructor = jvm->GetMethodID(TranslationHypothesisClass, "<init>",
//                                                            "(Ljava/lang/String;FLjava/util/List;)V");
//    }
//
//    // Create
//    std::vector<Feature> scores = hypothesis.getScores();
//
//    jstring jtext = jvm->NewStringUTF(hypothesis.getText().c_str());
//    jfloat jtotalScore = (jfloat) hypothesis.getTotalScore();
//    jobject jscores = jni_arraylist(jvm, scores.size());
//
//    for (size_t i = 0; i < scores.size(); ++i) {
//        jobject jfeature = create_MosesFeature(jvm, scores[i]);
//        jni_arraylist_add(jvm, jscores, jfeature);
//        jvm->DeleteLocalRef(jfeature);
//    }
//
//    jobject jhypothesis = jvm->NewObject(TranslationHypothesisClass, TranslationHypothesisConstructor, jtext,
//                                         jtotalScore, jscores);
//
//    jvm->DeleteLocalRef(jtext);
//    jvm->DeleteLocalRef(jscores);
//
//    return jhypothesis;
//}
//
//jobject create_Translation(JNIEnv *jvm, Translation translation) {
//    static jclass TranslationClass = NULL;
//    static jmethodID TranslationConstructor = NULL;
//
//    if (TranslationClass == NULL) {
//        TranslationClass = JNILoadClass(jvm, kTranslationClassName);
//        TranslationConstructor = jvm->GetMethodID(TranslationClass, "<init>", "(Ljava/lang/String;Ljava/util/List;)V");
//    }
//
//    // Create
//    std::vector<TranslationHypothesis> hypotheses = translation.getHypotheses();
//
//    jstring jtext = jvm->NewStringUTF(translation.getText().c_str());
//    jobject jhypotheses = jni_arraylist(jvm, hypotheses.size());
//
//    for (size_t i = 0; i < hypotheses.size(); ++i) {
//        jobject jhypothesis = create_TranslationHypothesis(jvm, hypotheses[i]);
//        jni_arraylist_add(jvm, jhypotheses, jhypothesis);
//        jvm->DeleteLocalRef(jhypothesis);
//    }
//
//    jobject jtranslation = jvm->NewObject(TranslationClass, TranslationConstructor, jtext, jhypotheses);
//
//    jvm->DeleteLocalRef(jtext);
//    jvm->DeleteLocalRef(jhypotheses);
//
//    return jtranslation;
//}
//
//// Public interface
//
//JNIEXPORT jobject JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_translate(JNIEnv *jvm, jobject self, jstring text,
//                                                                                jlong session,
//                                                                                jobject translationContext,
//                                                                                jint nbestListSize) {
//    std::string sentence = jni_jstrtostr(jvm, text);
//
//    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);
//
//    Translation translation;
//    if (translationContext != NULL) {
//        std::map<std::string, float> context = parse_context(jvm, translationContext);
//        translation = instance->translate(sentence, (uint64_t) session, &context, (size_t) nbestListSize);
//    } else {
//        translation = instance->translate(sentence, (uint64_t) session, NULL, (size_t) nbestListSize);
//    }
//
//    return create_Translation(jvm, translation);
//}
