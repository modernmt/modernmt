//
// Created by Davide  Caroselli on 03/12/15.
//

#include "JMosesFeature.h"

static JMosesFeature *__JMosesFeature_instance = NULL;

JMosesFeature::JMosesFeature(JNIEnv *jvm) : _class(jvm->FindClass("eu/modernmt/decoder/phrasebased/MosesFeature")) {
    constructor = jvm->GetMethodID(_class, "<init>", "(Ljava/lang/String;ZZJ)V");

    jfieldID field = jvm->GetStaticFieldID(_class, "UNTUNEABLE_COMPONENT", "F");
    UNTUNEABLE_COMPONENT = jvm->GetStaticFloatField(_class, field);
}

jobject JMosesFeature::create(JNIEnv *jvm, std::string &name, bool tunable, bool stateless, void *ptr) {
    jstring jname = jvm->NewStringUTF(name.c_str());
    jobject jfeature = jvm->NewObject(_class, constructor, jname, (jboolean) tunable, (jboolean) stateless,
                                      (jlong) ptr);
    jvm->DeleteLocalRef(jname);

    return jfeature;
}
