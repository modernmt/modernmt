//
// Created by Davide Caroselli on 26/07/16.
//

#ifndef MMTCORE_IDGENERATOR_H
#define MMTCORE_IDGENERATOR_H

#include <string>
#include <mutex>
#include <cstdint>

using namespace std;

class IdGenerator {
public:
    IdGenerator(string &filepath, uint32_t idStep = 1000);

    ~IdGenerator();

    uint32_t Next();

    void Reset(uint32_t id);

private:
    uint32_t idStep;
    uint32_t counter;
    FILE *storage;
    mutex m;
};


#endif //MMTCORE_IDGENERATOR_H
