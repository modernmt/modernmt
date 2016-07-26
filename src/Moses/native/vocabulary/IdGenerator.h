//
// Created by Davide Caroselli on 26/07/16.
//

#ifndef MOSESDECODER_IDGENERATOR_H
#define MOSESDECODER_IDGENERATOR_H

#include <string>
#include <mutex>
#include <cstdint>

using namespace std;

class IdGenerator {
public:
    IdGenerator(string &filepath, uint32_t idStep = 1000);

    ~IdGenerator();

    uint32_t Next();

private:
    uint32_t idStep;
    uint32_t counter;
    FILE *storage;
    mutex m;
};


#endif //MOSESDECODER_IDGENERATOR_H
