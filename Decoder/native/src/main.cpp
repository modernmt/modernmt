#include <iostream>
#include <wrapper/MosesDecoder.h>

using namespace std;
using namespace JNIWrapper;

MosesDecoder *_test_instance(const char *inifile) {
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

int main() {
    MosesDecoder *moses = _test_instance("/Users/davide/workspaces/mmt/ModernMT/engines/default/runtime/moses.ini");
    //cout << "hello world" << endl;

    // Test translation
    map<string, float> context;
    context["europarl"] = 1.f;

    Translation translation = moses->translate("aperto", 0L, &context, 5);
    cout << "Translation: " << translation.getText() << endl;
    cout << "Session: " << translation.getSession() << endl;

    vector<TranslationHypothesis> hypotheses = translation.getHypotheses();
    cout << "Hypotheses: (" << hypotheses.size() << ")" << endl;
    for (size_t i = 0; i < hypotheses.size(); ++i) {
        TranslationHypothesis hypothesis = hypotheses[i];

        cout << "\t" << hypothesis.getText() << " " << hypothesis.getTotalScore() << " - ";

        vector<Feature> features = hypothesis.getScores();
        for (size_t j = 0; j < features.size(); ++j) {
            Feature feature = features[j];
            cout << feature.getName() << " ";

            vector<float> scores = feature.getWeights();
            for (size_t k = 0; k < scores.size(); ++k) {
                cout << scores[k] << " ";
            }
        }

        cout << endl;
    }

    return 0;
}