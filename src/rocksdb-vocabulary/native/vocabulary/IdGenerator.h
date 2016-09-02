//
// Created by Davide Caroselli on 26/07/16.
//

#ifndef MMTCORE_IDGENERATOR_H
#define MMTCORE_IDGENERATOR_H

#include <string>
#include <mutex>
#include <cstdint>
#include <mmt/sentence.h>

using namespace std;

namespace mmt {
    namespace vocabulary {

        class IdGenerator {
        public:
            IdGenerator(string &filepath, wid_t idStep = 1000);

            ~IdGenerator();

            wid_t Next();

            void Reset(wid_t id);

        private:
            wid_t idStep;
            wid_t counter;
            FILE *storage;
            mutex m;
        };

    }
}

#endif //MMTCORE_IDGENERATOR_H
