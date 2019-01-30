//
// Created by Davide  Caroselli on 2019-01-30.
//

#ifndef MMT_FASTALIGN_IOUTILS_H
#define MMT_FASTALIGN_IOUTILS_H

#include <iostream>
#include <string>

inline void io_write(std::ostream &out, const std::string &value) {
    auto size = (uint32_t) value.size();
    out.write((const char *) &size, sizeof(uint32_t));
    out.write(&value[0], size);
}

inline void io_read(std::istream &in, std::string &out) {
    uint32_t size;
    in.read((char *) &size, sizeof(uint32_t));

    out.resize(size);
    in.read(&out[0], size);
}

template<typename T>
inline void io_write(std::ostream &out, T value) {
    out.write((const char *)&value, sizeof(T));
}

template<typename T>
inline T io_read(std::istream &in) {
    T value;
    in.read((char *) &value, sizeof(T));
    return value;
}

#endif //MMT_FASTALIGN_IOUTILS_H
