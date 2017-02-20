//
// Created by Davide  Caroselli on 08/02/17.
//

#include <javah/eu_modernmt_decoder_moses_NativeDataListener.h>
#include <mmt/IncrementalModel.h>
#include <mmt/jniutil.h>

using namespace std;
using namespace mmt;

void ParseSentence(JNIEnv *jvm, jintArray jsentence, vector<wid_t> &outSentence) {
    size_t size = (size_t) jvm->GetArrayLength(jsentence);

    outSentence.resize(size);

    jint *jsentenceArray = jvm->GetIntArrayElements(jsentence, 0);
    for (size_t i = 0; i < size; ++i)
        outSentence[i] = (wid_t) jsentenceArray[i];

    jvm->ReleaseIntArrayElements(jsentence, jsentenceArray, 0);
}

void ParseAlignment(JNIEnv *jvm, jintArray jalignment, alignment_t &outAlignment) {
    size_t fullsize = (size_t) jvm->GetArrayLength(jalignment);
    size_t size = fullsize / 2;

    jint *jalignmentArray = jvm->GetIntArrayElements(jalignment, 0);

    outAlignment.resize(size);

    for (size_t i = 0; i < size; ++i) {
        outAlignment[i].first = (length_t) jalignmentArray[i];
        outAlignment[i].second = (length_t) jalignmentArray[i + size];
    }

    jvm->ReleaseIntArrayElements(jalignment, jalignmentArray, 0);
}

/*
 * Class:     eu_modernmt_decoder_moses_NativeDataListener
 * Method:    updateReceived
 * Signature: (SJI[I[I[I)V
 */
JNIEXPORT void JNICALL Java_eu_modernmt_decoder_moses_NativeDataListener_updateReceived
        (JNIEnv *jvm, jobject jself, jshort jchannel, jlong jchannelPosition,
         jint jdomain, jintArray jsource, jintArray jtarget, jintArray jalignment) {
    IncrementalModel *instance = jni_gethandle<IncrementalModel>(jvm, jself);

    updateid_t id((stream_t) jchannel, (seqid_t) jchannelPosition);
    domain_t domain = (domain_t) jdomain;

    vector<wid_t> source;
    ParseSentence(jvm, jsource, source);

    vector<wid_t> target;
    ParseSentence(jvm, jtarget, target);

    alignment_t alignment;
    ParseAlignment(jvm, jalignment, alignment);

    instance->Add(id, domain, source, target, alignment);
}

/*
 * Class:     eu_modernmt_decoder_moses_NativeDataListener
 * Method:    deleteReceived
 * Signature: (SJI)V
 */
JNIEXPORT void JNICALL Java_eu_modernmt_decoder_moses_NativeDataListener_deleteReceived
        (JNIEnv *jvm, jobject jself, jshort jchannel, jlong jchannelPosition, jint jdomain) {
    IncrementalModel *instance = jni_gethandle<IncrementalModel>(jvm, jself);

    updateid_t id((stream_t) jchannel, (seqid_t) jchannelPosition);
    domain_t domain = (domain_t) jdomain;

    instance->Delete(id, domain);
}

/*
 * Class:     eu_modernmt_decoder_moses_NativeDataListener
 * Method:    getLatestUpdatesIdentifier
 * Signature: ()[J
 */
JNIEXPORT jlongArray JNICALL Java_eu_modernmt_decoder_moses_NativeDataListener_getLatestUpdatesIdentifier
        (JNIEnv *jvm, jobject jself) {
    IncrementalModel *instance = jni_gethandle<IncrementalModel>(jvm, jself);

    unordered_map<stream_t, seqid_t> ids = instance->GetLatestUpdatesIdentifier();

    vector<jlong> jidsArray;
    for (auto id = ids.begin(); id != ids.end(); ++id) {
        mmt::stream_t stream = id->first;

        if (stream >= jidsArray.size())
            jidsArray.resize(((size_t) stream) + 1, -1);

        jidsArray[stream] = (jlong) id->second;
    }

    jsize size = (jsize) jidsArray.size();

    jlongArray jarray = jvm->NewLongArray(size);
    jvm->SetLongArrayRegion(jarray, 0, size, jidsArray.data());

    return jarray;
}
