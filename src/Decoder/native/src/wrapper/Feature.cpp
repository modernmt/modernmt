//
// Created by Davide  Caroselli on 30/11/15.
//

#include "Feature.h"
#include "moses/StaticData.h"

using namespace JNIWrapper;

void Feature::initFromFeature(const Moses::FeatureFunction &featureFunction) {
    name = featureFunction.GetScoreProducerDescription();
    tuneable = featureFunction.IsTuneable();
    weights.clear();

    if (this->tuneable) {
        weights = Moses::StaticData::Instance().GetAllWeights().GetScoresForProducer(&featureFunction);

        for (size_t i = 0; i < featureFunction.GetNumScoreComponents(); ++i) {
            if (!featureFunction.IsTuneableComponent(i)) {
                weights[i] = UNTUNEABLE;
            }
        }
    }
}
