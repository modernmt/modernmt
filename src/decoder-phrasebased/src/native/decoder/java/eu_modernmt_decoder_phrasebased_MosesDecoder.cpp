//
// Created by Davide  Caroselli on 26/11/15.
//

#include "../javah/eu_modernmt_decoder_phrasebased_MosesDecoder.h"
#include "../moses/MosesDecoder.h"
#include <stdlib.h>
#include "JMosesFeature.h"
#include "JTranslation.h"
#include <mmt/jniutil.h>
#include <moses/MosesDecoder.h>

using namespace std;
using namespace mmt;
using namespace mmt::decoder;

void ParseContext(JNIEnv *jvm, jlongArray keys, jfloatArray values, map<string, float> &outContext) {
    int size = jvm->GetArrayLength(values);

    jlong *keysArray = jvm->GetLongArrayElements(keys, 0);
    jfloat *valuesArray = jvm->GetFloatArrayElements(values, 0);

    for (int i = 0; i < size; i++) {
        string key = std::to_string((domain_t) keysArray[i]);
        float value = valuesArray[i];

        outContext[key] = value;
    }

    jvm->ReleaseLongArrayElements(keys, keysArray, 0);
    jvm->ReleaseFloatArrayElements(values, valuesArray, 0);
}

alignment_t ParseAlignment(JNIEnv *jvm, jintArray jalignment) {
    size_t fullsize = (size_t) jvm->GetArrayLength(jalignment);
    size_t size = fullsize / 2;

    jint *jalignmentArray = jvm->GetIntArrayElements(jalignment, 0);

    alignment_t result(size);

    for (size_t i = 0; i < size; ++i) {
        result[i].first = (length_t) jalignmentArray[i];
        result[i].second = (length_t) jalignmentArray[i + size];
    }

    jvm->ReleaseIntArrayElements(jalignment, jalignmentArray, 0);

    return result;
}

/*
 * Class:     eu_modernmt_decoder_phrasebased_MosesDecoder
 * Method:    instantiate
 * Signature: (Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_eu_modernmt_decoder_phrasebased_MosesDecoder_instantiate(JNIEnv *jvm, jobject jself, jstring jinifile,
                                                              jstring jvocabulary) {
    string inifile = jni_jstrtostr(jvm, jinifile);
    string vocabulary = jni_jstrtostr(jvm, jvocabulary);
    return (jlong) MosesDecoder::createInstance(inifile, vocabulary);
}

/*
 * Class:     eu_modernmt_decoder_phrasebased_MosesDecoder
 * Method:    features
 * Signature: ()[Leu/modernmt/decoder/phrasebased/MosesFeature;
 */
JNIEXPORT jobjectArray JNICALL
Java_eu_modernmt_decoder_phrasebased_MosesDecoder_features(JNIEnv *jvm, jobject jself) {
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
 * Class:     eu_modernmt_decoder_phrasebased_MosesDecoder
 * Method:    getFeatureWeightsFromPointer
 * Signature: (J)[F
 */
JNIEXPORT jfloatArray JNICALL
Java_eu_modernmt_decoder_phrasebased_MosesDecoder_getFeatureWeightsFromPointer(JNIEnv *jvm, jobject jself,
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

/*
 * Class:     eu_modernmt_decoder_phrasebased_MosesDecoder
 * Method:    setFeatureWeights
 * Signature: ([Ljava/lang/String;[[F)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_decoder_phrasebased_MosesDecoder_setFeatureWeights(JNIEnv *jvm, jobject self, jobjectArray features,
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
 * Class:     eu_modernmt_decoder_phrasebased_MosesDecoder
 * Method:    xtranslate
 * Signature: (Ljava/lang/String;[J[FI)Leu/modernmt/decoder/phrasebased/TranslationXObject;
 */
JNIEXPORT jobject JNICALL Java_eu_modernmt_decoder_phrasebased_MosesDecoder_xtranslate
        (JNIEnv *jvm, jobject jself, jstring text, jlongArray contextKeys, jfloatArray contextValues, jint nbest) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, jself);
    string sentence = jni_jstrtostr(jvm, text);

    translation_t translation;
    if (contextKeys != NULL) {
        map<string, float> context;
        ParseContext(jvm, contextKeys, contextValues, context);

        translation = instance->translate(sentence, &context, (size_t) nbest);
    } else {
        translation = instance->translate(sentence, NULL, (size_t) nbest);
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
 * Class:     eu_modernmt_decoder_phrasebased_MosesDecoder
 * Method:    updateReceived
 * Signature: ([S[J[J[Ljava/lang/String;[Ljava/lang/String;[[I)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_decoder_phrasebased_MosesDecoder_updateReceived(JNIEnv *jvm, jobject jself, jshortArray jchannels,
                                                                 jlongArray jpositions, jlongArray jdomains,
                                                                 jobjectArray jsources, jobjectArray jtargets,
                                                                 jobjectArray jalignments) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, jself);

    size_t size = (size_t) jvm->GetArrayLength(jchannels);
    vector<translation_unit_t> batch(size);

    jshort *jchannelsArray = jvm->GetShortArrayElements(jchannels, 0);
    jlong *jpositionsArray = jvm->GetLongArrayElements(jpositions, 0);
    jlong *jdomainsArray = jvm->GetLongArrayElements(jdomains, 0);

    for (size_t i = 0; i < size; ++i) {
        translation_unit_t &unit = batch[i];
        unit.id = updateid_t((stream_t) jchannelsArray[i], (seqid_t) jpositionsArray[i]);
        unit.domain = (domain_t) jdomainsArray[i];
        unit.source = jni_jstrtostr(jvm, (jstring) jvm->GetObjectArrayElement(jsources, (jsize) i));
        unit.target = jni_jstrtostr(jvm, (jstring) jvm->GetObjectArrayElement(jtargets, (jsize) i));
        unit.alignment = ParseAlignment(jvm, (jintArray) jvm->GetObjectArrayElement(jalignments, (jsize) i));
    }

    jvm->ReleaseShortArrayElements(jchannels, jchannelsArray, 0);
    jvm->ReleaseLongArrayElements(jpositions, jpositionsArray, 0);
    jvm->ReleaseLongArrayElements(jdomains, jdomainsArray, 0);

    instance->DeliverUpdates(batch);
}

/*
 * Class:     eu_modernmt_decoder_phrasebased_MosesDecoder
 * Method:    deleteReceived
 * Signature: (SJJ)V
 */
JNIEXPORT void JNICALL
Java_eu_modernmt_decoder_phrasebased_MosesDecoder_deleteReceived(JNIEnv *jvm, jobject jself, jshort jchannel,
                                                                 jlong jchannelPosition, jlong jdomain) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, jself);

    updateid_t id((stream_t) jchannel, (seqid_t) jchannelPosition);
    domain_t domain = (domain_t) jdomain;

    instance->DeliverDeletion(id, domain);
}

/*
 * Class:     eu_modernmt_decoder_phrasebased_MosesDecoder
 * Method:    getLatestUpdatesIdentifier
 * Signature: ()[J
 */
JNIEXPORT jlongArray JNICALL
Java_eu_modernmt_decoder_phrasebased_MosesDecoder_getLatestUpdatesIdentifier(JNIEnv *jvm, jobject jself) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, jself);

    unordered_map<stream_t, seqid_t> ids = instance->GetLatestUpdatesIdentifiers();

    vector<jlong> jidsArray(ids.size() * 2);
    size_t i = 0;
    for (auto entry = ids.begin(); entry != ids.end(); ++entry) {
        jidsArray[i++] = (jlong) entry->first;
        jidsArray[i++] = (jlong) entry->second;
    }

    jsize size = (jsize) jidsArray.size();

    jlongArray jarray = jvm->NewLongArray(size);
    jvm->SetLongArrayRegion(jarray, 0, size, jidsArray.data());

    return jarray;
}

/*
 * Class:     eu_modernmt_decoder_phrasebased_MosesDecoder
 * Method:    dispose
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_eu_modernmt_decoder_phrasebased_MosesDecoder_dispose(JNIEnv *jvm, jobject jself, jlong ptr) {
    if (ptr != 0) {
        MosesDecoder *instance = (MosesDecoder *) ptr;
        delete instance;
    }

    return 0;
}