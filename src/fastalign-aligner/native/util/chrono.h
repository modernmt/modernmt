//
// Created by Davide Caroselli on 30/09/16.
//

#ifndef FASTALIGN_CHRONO_H
#define FASTALIGN_CHRONO_H

#include <sys/time.h>

inline double GetTime() {
    struct timeval time;

    if (gettimeofday(&time, NULL)) {
        //  Handle error
        return 0;
    }

    return (double) time.tv_sec + ((double) time.tv_usec / 1000000.);
}

inline double GetElapsedTime(double begin) {
    return GetTime() - begin;
}

#endif //FASTALIGN_CHRONO_H
