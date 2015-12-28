#include <iostream>
#include <wrapper/MosesDecoder.h>

using namespace std;
using namespace JNIWrapper;

int main() {
    MosesDecoder *moses = MosesDecoder::createInstance("/home/davide/ModernMT/engines/default/runtime/moses.ini");
    //cout << "hello world" << endl;

    vector<feature_t> features = moses->getFeatures();
    cout << features.size() << endl;

    return 0;
}