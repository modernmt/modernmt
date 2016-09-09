//
// Created by Davide  Caroselli on 03/12/15.
//

#ifndef JNIMOSES_JMOSESFEATURE_H
#define JNIMOSES_JMOSESFEATURE_H

#include <jni.h>
#include <string>

class JMosesFeature {
    jmethodID constructor;

public:
    const jclass _class;
    jfloat UNTUNEABLE_COMPONENT;

    JMosesFeature(JNIEnv *);

    jobject create(JNIEnv *jvm, std::string &name, bool tunable, bool stateless, void *ptr);

};


#endif //JNIMOSES_JMOSESFEATURE_H
