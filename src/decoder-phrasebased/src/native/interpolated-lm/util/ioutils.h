//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_IOUTILS_H
#define SAPT_IOUTILS_H

#include <cstdint>

static inline void WriteUInt16(char *buffer, size_t *ptr, uint16_t value) {
    buffer[*ptr] = (char) (value & 0xFF);
    buffer[*ptr + 1] = (char) ((value >> 8) & 0xFF);

    *ptr = *ptr + 2;
}

static inline void WriteUInt32(char *buffer, size_t *ptr, uint32_t value) {
    buffer[*ptr] = (char) (value & 0xFF);
    buffer[*ptr + 1] = (char) ((value >> 8) & 0xFF);
    buffer[*ptr + 2] = (char) ((value >> 16) & 0xFF);
    buffer[*ptr + 3] = (char) ((value >> 24) & 0xFF);

    *ptr = *ptr + 4;
}

static inline void WriteInt64(char *buffer, size_t i, int64_t value) {
    buffer[i] = (char) (value & 0xFF);
    buffer[i + 1] = (char) ((value >> 8) & 0xFF);
    buffer[i + 2] = (char) ((value >> 16) & 0xFF);
    buffer[i + 3] = (char) ((value >> 24) & 0xFF);
    buffer[i + 4] = (char) ((value >> 32) & 0xFF);
    buffer[i + 5] = (char) ((value >> 40) & 0xFF);
    buffer[i + 6] = (char) ((value >> 48) & 0xFF);
    buffer[i + 7] = (char) ((value >> 56) & 0xFF);
}

static inline void WriteInt64(char *buffer, size_t *ptr, int64_t value) {
    buffer[*ptr] = (char) (value & 0xFF);
    buffer[*ptr + 1] = (char) ((value >> 8) & 0xFF);
    buffer[*ptr + 2] = (char) ((value >> 16) & 0xFF);
    buffer[*ptr + 3] = (char) ((value >> 24) & 0xFF);
    buffer[*ptr + 4] = (char) ((value >> 32) & 0xFF);
    buffer[*ptr + 5] = (char) ((value >> 40) & 0xFF);
    buffer[*ptr + 6] = (char) ((value >> 48) & 0xFF);
    buffer[*ptr + 7] = (char) ((value >> 56) & 0xFF);

    *ptr = *ptr + 8;
}

static inline void WriteUInt64(char *buffer, size_t *ptr, uint64_t value) {
    WriteInt64(buffer, ptr, value);
}

static inline void WriteUInt64(char *buffer, size_t ptr, uint64_t value) {
    WriteInt64(buffer, ptr, value);
}

static inline uint16_t ReadUInt16(const char *data, size_t *ptr) {
    uint16_t value = (uint16_t) ((data[*ptr] & 0xFFL) +
                                 ((data[*ptr + 1] & 0xFFL) << 8));
    *ptr = *ptr + 2;
    return value;
}

static inline uint16_t ReadUInt16(const char *data, size_t i) {
    return (uint16_t) ((data[i] & 0xFFL) +
                       ((data[i + 1] & 0xFFL) << 8));
}

static inline uint32_t ReadUInt32(const char *data, size_t *ptr) {
    uint32_t value = (uint32_t) ((data[*ptr] & 0xFFUL) +
                                 ((data[*ptr + 1] & 0xFFUL) << 8) +
                                 ((data[*ptr + 2] & 0xFFUL) << 16) +
                                 ((data[*ptr + 3] & 0xFFUL) << 24));
    *ptr = *ptr + 4;
    return value;
}

static inline uint32_t ReadUInt32(const char *data, size_t i) {
    return (uint32_t) ((data[i] & 0xFFUL) +
                       ((data[i + 1] & 0xFFUL) << 8) +
                       ((data[i + 2] & 0xFFUL) << 16) +
                       ((data[i + 3] & 0xFFUL) << 24));
}

static inline uint64_t ReadUInt64(const char *data, size_t *ptr) {
    uint64_t value = (data[*ptr] & 0xFFUL) +
                     ((data[*ptr + 1] & 0xFFUL) << 8) +
                     ((data[*ptr + 2] & 0xFFUL) << 16) +
                     ((data[*ptr + 3] & 0xFFUL) << 24) +
                     ((data[*ptr + 4] & 0xFFUL) << 32) +
                     ((data[*ptr + 5] & 0xFFUL) << 40) +
                     ((data[*ptr + 6] & 0xFFUL) << 48) +
                     ((data[*ptr + 7] & 0xFFUL) << 56);
    *ptr = *ptr + 8;
    return value;
}

static inline int64_t ReadInt64(const char *data, size_t *ptr) {
    return ReadUInt64(data, ptr);
}

static inline uint64_t ReadUInt64(const char *data, size_t i) {
    return (data[i] & 0xFFUL) +
           ((data[i + 1] & 0xFFUL) << 8) +
           ((data[i + 2] & 0xFFUL) << 16) +
           ((data[i + 3] & 0xFFUL) << 24) +
           ((data[i + 4] & 0xFFUL) << 32) +
           ((data[i + 5] & 0xFFUL) << 40) +
           ((data[i + 6] & 0xFFUL) << 48) +
           ((data[i + 7] & 0xFFUL) << 56);
}

static inline int64_t ReadInt64(const char *data, size_t i) {
    return ReadUInt64(data, i);
}

// Variable byte encoding

static inline size_t VBEWriteUInt32(char *buffer, size_t *ptr, uint32_t value) {
    size_t i;
    for (i = 0; i < 5; ++i) {
        buffer[(*ptr)++] = (char) (value & 0b1111111);
        value = value >> 7;

        if (value == 0)
            break;
    }

    buffer[*ptr - 1] |= 0b10000000;
    return i + 1;
}

static inline uint32_t VBEReadUInt32(const char *buffer, size_t *ptr) {
    uint32_t result = 0;

    for (size_t i = 0; i < 5; ++i) {
        char byte = buffer[(*ptr)++];
        result |= (byte & 0b1111111) << (i * 7);

        if (byte & 0b10000000)
            break;
    }

    return result;
}

static inline size_t VBELengthOfUInt32(uint32_t value) {
    if (value >= 0x10000000) //    0b10000000000000000000000000000
        return  5;
    else if (value >= 0x200000) // 0b1000000000000000000000
        return 4;
    else if (value >= 0x4000) //   0b100000000000000
        return 3;
    else if (value >= 0x80) //     0b10000000
        return 2;
    else
        return 1;
}

#endif //SAPT_IOUTILS_H
