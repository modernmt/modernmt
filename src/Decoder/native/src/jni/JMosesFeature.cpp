//
// Created by Davide  Caroselli on 03/12/15.
//

#include "JMosesFeature.h"
#include "jconv.h"

static JMosesFeature *__instance = NULL;

JMosesFeature::JMosesFeature(JNIEnv *jvm) : _class(JNILoadClass(jvm, "eu/modernmt/decoder/moses/MosesFeature")) {
    constructor = jvm->GetMethodID(_class, "<init>", "(Ljava/lang/String;ZZJ)V");
    ptr = jvm->GetFieldID(_class, "ptr", "J");

    jfieldID field = jvm->GetStaticFieldID(_class, "UNTUNEABLE_COMPONENT", "F");
    UNTUNEABLE_COMPONENT = jvm->GetStaticFloatField(_class, field);
}

JMosesFeature *JMosesFeature::instance(JNIEnv *jvm) {
    if (__instance == NULL)
        __instance = new JMosesFeature(jvm);

    return __instance;
}

jobject JMosesFeature::create(JNIEnv *jvm, std::string &name, bool tunable, bool stateless, void *ptr) {
    jstring jname = jvm->NewStringUTF(name.c_str());
    jobject jfeature = jvm->NewObject(_class, constructor, jname, (jboolean) tunable, (jboolean) stateless,
                                      (jlong) ptr);
    jvm->DeleteLocalRef(jname);

    return jfeature;
}

void *JMosesFeature::getPtr(JNIEnv *jvm, jobject self) {
    return (void *) jvm->GetLongField(self, ptr);
}
