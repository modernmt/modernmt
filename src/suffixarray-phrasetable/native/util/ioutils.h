//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_IOUTILS_H
#define SAPT_IOUTILS_H

#include <cstdint>

static inline void WriteInt16(char *buffer, int64_t *ptr, uint16_t value) {
    buffer[*ptr] = (char) (value & 0xFF);
    buffer[*ptr + 1] = (char) ((value >> 8) & 0xFF);

    *ptr = *ptr + 2;
}

static inline void WriteInt32(char *buffer, int64_t *ptr, uint32_t value) {
    buffer[*ptr] = (char) (value & 0xFF);
    buffer[*ptr + 1] = (char) ((value >> 8) & 0xFF);
    buffer[*ptr + 2] = (char) ((value >> 16) & 0xFF);
    buffer[*ptr + 3] = (char) ((value >> 24) & 0xFF);

    *ptr = *ptr + 4;
}

static inline uint16_t ReadInt16(const char *data, int64_t *ptr) {
    uint16_t value = (uint16_t) ((data[*ptr] & 0xFFL) +
                                 ((data[*ptr + 1] & 0xFFL) << 8));
    *ptr = *ptr + 2;
    return value;
}

static inline uint32_t ReadInt32(const char *data, int64_t *ptr) {
    uint32_t value = (uint32_t) ((data[*ptr] & 0xFFUL) +
                                 ((data[*ptr + 1] & 0xFFUL) << 8) +
                                 ((data[*ptr + 2] & 0xFFUL) << 16) +
                                 ((data[*ptr + 3] & 0xFFUL) << 24));
    *ptr = *ptr + 4;
    return value;
}

#endif //SAPT_IOUTILS_H
