//
// Created by Davide  Caroselli on 26/11/15.
//

#include <eu_modernmt_decoder_moses_MosesDecoder.h>
#include <jni/handle.h>
#include <jni/jconv.h>
#include <wrapper/MosesDecoder.h>

using namespace JNIWrapper;

static const char *kTranslationHypothesisClassName = "eu/modernmt/decoder/TranslationHypothesis";
static const char *kMosesFeatureClassName = "eu/modernmt/decoder/moses/MosesFeature";
static const char *kTranslationClassName = "eu/modernmt/decoder/Translation";

// Utils

MosesDecoder *new_instance(const char *inifile) {
    const char *argv[2] = {"-f", inifile};

    Moses::Parameter params;

    if (!params.LoadParam(2, argv))
        return NULL;

    // initialize all "global" variables, which are stored in StaticData
    // note: this also loads models such as the language model, etc.
    if (!Moses::StaticData::LoadDataStatic(&params, "moses"))
        return NULL;

    return new MosesDecoder(params);
}

std::map<std::string, float> parse_context(JNIEnv *jvm, jobject jcontext) {
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

jobject create_MosesFeature(JNIEnv *jvm, Feature &feature) {
    static jclass MosesFeatureClass = NULL;
    static jmethodID MosesFeatureConstructor = NULL;
    static jfloat JUNTUNEABLE = 0.f;

    if (MosesFeatureClass == NULL) {
        MosesFeatureClass = JNILoadClass(jvm, kMosesFeatureClassName);
        MosesFeatureConstructor = jvm->GetMethodID(MosesFeatureClass, "<init>", "(Ljava/lang/String;[F)V");
        JUNTUNEABLE = jvm->GetStaticFloatField(MosesFeatureClass,
                                               jvm->GetStaticFieldID(MosesFeatureClass, "UNTUNEABLE", "F"));
    }

    // Create
    std::vector<float> weights = feature.getWeights();

    jstring jname = jvm->NewStringUTF(feature.getName().c_str());
    size_t size = weights.size();
    jfloatArray jweights = NULL;

    if (size > 0) {
        jfloat *buffer = (jfloat *) calloc(sizeof(jfloat), size);

        for (size_t i = 0; i < size; ++i) {
            float w = weights[i];
            buffer[i] = (w == Feature::UNTUNEABLE ? JUNTUNEABLE : (jfloat) w);
        }


        jweights = jvm->NewFloatArray((jsize) size);
        jvm->SetFloatArrayRegion(jweights, 0, (jsize) size, buffer);
        free(buffer);
    }

    jobject jfeature = jvm->NewObject(MosesFeatureClass, MosesFeatureConstructor, jname, jweights);

    jvm->DeleteLocalRef(jname);
    jvm->DeleteLocalRef(jweights);

    return jfeature;
}

jobject create_TranslationHypothesis(JNIEnv *jvm, TranslationHypothesis hypothesis) {
    static jclass TranslationHypothesisClass = NULL;
    static jmethodID TranslationHypothesisConstructor = NULL;

    if (TranslationHypothesisClass == NULL) {
        TranslationHypothesisClass = JNILoadClass(jvm, kTranslationHypothesisClassName);
        TranslationHypothesisConstructor = jvm->GetMethodID(TranslationHypothesisClass, "<init>",
                                                            "(Ljava/lang/String;FLjava/util/List;)V");
    }

    // Create
    std::vector<Feature> scores = hypothesis.getScores();

    jstring jtext = jvm->NewStringUTF(hypothesis.getText().c_str());
    jfloat jtotalScore = (jfloat) hypothesis.getTotalScore();
    jobject jscores = jni_arraylist(jvm, scores.size());

    for (size_t i = 0; i < scores.size(); ++i) {
        jobject jfeature = create_MosesFeature(jvm, scores[i]);
        jni_arraylist_add(jvm, jscores, jfeature);
        jvm->DeleteLocalRef(jfeature);
    }

    jobject jhypothesis = jvm->NewObject(TranslationHypothesisClass, TranslationHypothesisConstructor, jtext,
                                         jtotalScore, jscores);

    jvm->DeleteLocalRef(jtext);
    jvm->DeleteLocalRef(jscores);

    return jhypothesis;
}

jobject create_Translation(JNIEnv *jvm, Translation translation) {
    static jclass TranslationClass = NULL;
    static jmethodID TranslationConstructor = NULL;

    if (TranslationClass == NULL) {
        TranslationClass = JNILoadClass(jvm, kTranslationClassName);
        TranslationConstructor = jvm->GetMethodID(TranslationClass, "<init>", "(Ljava/lang/String;Ljava/util/List;)V");
    }

    // Create
    std::vector<TranslationHypothesis> hypotheses = translation.getHypotheses();

    jstring jtext = jvm->NewStringUTF(translation.getText().c_str());
    jobject jhypotheses = jni_arraylist(jvm, hypotheses.size());

    for (size_t i = 0; i < hypotheses.size(); ++i) {
        jobject jhypothesis = create_TranslationHypothesis(jvm, hypotheses[i]);
        jni_arraylist_add(jvm, jhypotheses, jhypothesis);
        jvm->DeleteLocalRef(jhypothesis);
    }

    jobject jtranslation = jvm->NewObject(TranslationClass, TranslationConstructor, jtext, jhypotheses);

    jvm->DeleteLocalRef(jtext);
    jvm->DeleteLocalRef(jhypotheses);

    return jtranslation;
}

// Public interface

JNIEXPORT void JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_init(JNIEnv *jvm, jobject self, jstring mosesIni) {
    const char *inifile = jvm->GetStringUTFChars(mosesIni, NULL);
    MosesDecoder *instance = new_instance(inifile);
    jvm->ReleaseStringUTFChars(mosesIni, inifile);

    jni_sethandle(jvm, self, instance);
}

JNIEXPORT jobject JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_getFeatureWeights(JNIEnv *jvm, jobject self) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);

    std::vector<Feature> features = instance->getFeatureWeights();
    jobject list = jni_arraylist(jvm, features.size());

    for (size_t i = 0; i < features.size(); ++i) {
        jobject jfeature = create_MosesFeature(jvm, features[i]);
        jni_arraylist_add(jvm, list, jfeature);
        jvm->DeleteLocalRef(jfeature);
    }

    return list;
}

JNIEXPORT jlong JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_createSession(JNIEnv *jvm, jobject self,
                                                                                  jobject translationContext) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);
    std::map<std::string, float> context = parse_context(jvm, translationContext);
    return (jlong) instance->openSession(context);
}

JNIEXPORT void JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_closeSession(JNIEnv *jvm, jobject self,
                                                                                jlong session) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);
    instance->closeSession((uint64_t) session);
}

JNIEXPORT jobject JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_translate(JNIEnv *jvm, jobject self, jstring text,
                                                                                jlong session,
                                                                                jobject translationContext,
                                                                                jint nbestListSize) {
    std::string sentence = jni_jstrtostr(jvm, text);

    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);

    Translation translation;
    if (translationContext != NULL) {
        std::map<std::string, float> context = parse_context(jvm, translationContext);
        translation = instance->translate(sentence, (uint64_t) session, &context, (size_t) nbestListSize);
    } else {
        translation = instance->translate(sentence, (uint64_t) session, NULL, (size_t) nbestListSize);
    }

    return create_Translation(jvm, translation);
}

JNIEXPORT void JNICALL Java_eu_modernmt_decoder_moses_MosesDecoder_dispose(JNIEnv *jvm, jobject self) {
    MosesDecoder *instance = jni_gethandle<MosesDecoder>(jvm, self);
    jni_sethandle<MosesDecoder>(jvm, self, 0);
    delete instance;
}