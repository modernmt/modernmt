//
// Created by Davide  Caroselli on 24/06/16.
//

#include "eu_modernmt_aligner_fastalign_FastAlign.h"
#include "handle.h"
#include "jniutil.h"
#include <src/da.h>
#include <sstream>
#include <src/ForceAlign.h>

JNIEXPORT void JNICALL Java_eu_modernmt_aligner_fastalign_FastAlign_init(JNIEnv *jvm, jobject jself, jstring jmodelPath,
                                                                         jboolean reverse) {
    std::string modelPath = jni_jstrtostr(jvm, jmodelPath);
    ForceAlign *model = new ForceAlign(reverse, 1);
    model->load_model(modelPath, true);
    jni_sethandle<ForceAlign>(jvm, jself, model);
}

/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    align
 * Signature: (Ljava/lang/String;Ljava/lang/String;)[[I
 */
JNIEXPORT jstring JNICALL Java_eu_modernmt_aligner_fastalign_FastAlign_align(JNIEnv *jvm, jobject jself,
                                                                             jstring jsource, jstring jtarget) {
    ForceAlign *model = jni_gethandle<ForceAlign>(jvm, jself);
    std::string source = jni_jstrtostr(jvm, jsource);
    std::string target = jni_jstrtostr(jvm, jtarget);

    std::string result = model->get_alignment(source, target);
    return jvm->NewStringUTF(result.c_str());
}

/*
 * Class:     eu_modernmt_aligner_fastalign_FastAlign
 * Method:    dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_eu_modernmt_aligner_fastalign_FastAlign_dispos(JNIEnv *jvm, jobject jself) {
    ForceAlign *model = jni_gethandle<ForceAlign>(jvm, jself);
    delete model;
}