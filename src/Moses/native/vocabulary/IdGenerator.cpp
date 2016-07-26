//
// Created by Davide Caroselli on 26/07/16.
//

#include <iostream>
#include "IdGenerator.h"

using namespace std;

static void write(uint32_t value, FILE *file) {
    uint8_t buffer[4];

    buffer[0] = (uint8_t) (value & 0x000000FF);
    buffer[1] = (uint8_t) ((value & 0x0000FF00) >> 8);
    buffer[2] = (uint8_t) ((value & 0x00FF0000) >> 16);
    buffer[3] = (uint8_t) ((value & 0xFF000000) >> 24);

    fseek(file, 0, SEEK_SET);
    fwrite((void *)buffer, sizeof(uint8_t), 4, file);
    fflush(file);
}

static uint32_t read(FILE *file) {
    fseek(file, 0, SEEK_SET);

    uint8_t buffer[4];
    fread((void *) buffer, sizeof(uint8_t), 4, file);

    uint32_t id = buffer[0] & 0xFFU;
    id += (buffer[1] & 0xFFU) << 8;
    id += (buffer[2] & 0xFFU) << 16;
    id += (buffer[3] & 0xFFU) << 24;

    return id;
}

IdGenerator::IdGenerator(string &filepath, uint32_t idStep) : idStep(idStep) {
    const char *cpath = filepath.c_str();

    if (FILE *file = fopen(cpath, "r")) {
        counter = read(file);
        fclose(file);
    } else {
        counter = 0;
    }

    uint32_t upperbound = ((uint32_t) (counter / idStep) + 1) * idStep;

    storage = fopen(cpath, "w+");
    write(upperbound, storage);
}

IdGenerator::~IdGenerator() {
    write(counter, storage);
    fclose(storage);
}

uint32_t IdGenerator::Next() {
    uint32_t result;

    m.lock();
    {
        result = counter++;

        if (result % idStep == 0)
            write(result + idStep, storage);
    };
    m.unlock();

    return result;
}
