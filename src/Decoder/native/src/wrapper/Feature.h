//
// Created by Davide  Caroselli on 30/11/15.
//

#ifndef JNIMOSES_MOSESFEATURE_H
#define JNIMOSES_MOSESFEATURE_H

#include <string>
#include <vector>
#include <float.h>
#include <moses/FF/FeatureFunction.h>

namespace JNIWrapper {
    class Feature {
        std::string name;
        std::vector<float> weights;
        bool tuneable;

    public:
        static constexpr float UNTUNEABLE = FLT_MAX;

        Feature() : name(), weights(), tuneable(false) { };

        void initFromFeature(const Moses::FeatureFunction &featureFunction);

        void setName(const std::string &name) {
            Feature::name = name;
        }

        void setTuneable(bool tuneable) {
            Feature::tuneable = tuneable;
        }

        const std::string &getName() const {
            return name;
        }

        std::vector<float> &getWeights() {
            return weights;
        }

        bool isTuneable() const {
            return tuneable;
        }
    };
}


#endif //JNIMOSES_MOSESFEATURE_H
