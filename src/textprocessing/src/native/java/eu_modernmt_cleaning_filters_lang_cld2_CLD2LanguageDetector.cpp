#include "javah/eu_modernmt_cleaning_filters_lang_cld2_CLD2LanguageDetector.h"
#include "jniutil.h"
#include "cld2/public/compact_lang_det.h"

using namespace std;

/*
 * Class:     eu_modernmt_cleaning_filters_lang_cld2_CLD2LanguageDetector
 * Method:    detectLanguage
 * Signature: ([BZ)I
 */
JNIEXPORT jint JNICALL
Java_eu_modernmt_cleaning_filters_lang_cld2_CLD2LanguageDetector_detectLanguage(JNIEnv *jvm, jobject jself,
                                                                                jbyteArray jtext,
                                                                                jboolean jreliableOnly) {
    string text = jni_jstrtostr(jvm, jtext);


    signed char *buffer = jvm->GetByteArrayElements(jtext, NULL);
    int length = (int) jvm->GetArrayLength(jtext);

    bool isReliable;
    CLD2::Language language = CLD2::DetectLanguage((const char *) buffer, length, true, &isReliable);

    jvm->ReleaseByteArrayElements(jtext, buffer, JNI_ABORT);

    return jreliableOnly && !isReliable ? -1 : (int) language;
}