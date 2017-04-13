//
// Created by Davide  Caroselli on 07/09/16.
//

#ifndef ILM_STATICLM_H
#define ILM_STATICLM_H

#include <lm/model.hh>
#include "LM.h"
#include "Options.h"

namespace mmt {
    namespace ilm {

        class StaticLM : public LM {
        public:

            static StaticLM *LoadFromPath(const string &modelPath,
                                          const Options::StaticLM &options = Options::StaticLM());

        };

    }
}


#endif //ILM_STATICLM_H
