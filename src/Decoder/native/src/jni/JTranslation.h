//
// Created by Davide  Caroselli on 09/12/15.
//

#ifndef JNIMOSES_JTRANSLATION_H
#define JNIMOSES_JTRANSLATION_H

#include <jni.h>
#include <string>
#include <vector>

class JTranslation {
    jmethodID constructor;
    JTranslation(JNIEnv *);

public:
    const jclass _class;

    static JTranslation *instance(JNIEnv *);

    jobject create(JNIEnv *jvm, std::string &text, jobjectArray nbestList);
};

class JHypothesis {
    jmethodID constructor;
    JHypothesis(JNIEnv *);

public:
    const jclass _class;

    static JHypothesis *instance(JNIEnv *);

    jobject create(JNIEnv *jvm, std::string &text, float totalScore, std::string &fvals);
};


#endif //JNIMOSES_JTRANSLATION_H
