//
// Created by Davide Caroselli on 04/09/16.
//

#include "SymAlignment.h"
#include <vector>
#include <cstring>
#include <cstdlib>

using namespace std;
using namespace mmt;
using namespace fastalign;

static const int kGrowDiagonalNeighbors[8][2] = {
        // Grow
        {-1, 0},
        {0,  -1},
        {1,  0},
        {0,  1},

        // Diagonal
        {-1, -1},
        {-1, 1},
        {1,  -1},
        {1,  1}
};

void SymAlignment::Reset(size_t _source_length, size_t _target_length) {
    source_length = _source_length;
    target_length = _target_length;

    size_t new_size = source_length * target_length;
    if (new_size > size) {
        data = (uint8_t *) realloc(data, new_size);
        size = new_size;
    }

    memset(data, 0, size);
}

void SymAlignment::Union(const alignment_t &forward, const alignment_t &backward) {
    Merge(forward, backward);
}

void SymAlignment::Intersection(const alignment_t &forward, const alignment_t &backward) {
    Merge(forward, backward);

    for (size_t i = 0; i < (source_length * target_length); ++i) {
        data[i] = (uint8_t) (data[i] == 0x03 ? 1 : 0);
    }
}

void SymAlignment::Grow(const alignment_t &forward, const alignment_t &backward, bool diagonal, bool final) {
    Merge(forward, backward);

    size_t neighbors_size = diagonal ? 8 : 4;

    bool added = true;
    while (added) {
        added = false;

        for (size_t s = 0; s < source_length; ++s) {
            for (size_t t = 0; t < target_length; ++t) {
                if ((data[idx(s, t)] & 0x03) == 0x03)
                    continue; // point is not in intersection/added

                for (size_t ni = 0; ni < neighbors_size; ++ni) {
                    size_t ns = s + kGrowDiagonalNeighbors[ni][0];
                    size_t nt = t + kGrowDiagonalNeighbors[ni][1];

                    if (ns >= source_length || nt >= target_length)
                        continue; // point is outside matrix

                    if ((!IsSourceWordAligned(ns) || !IsTargetWordAligned(nt)) &&
                        (data[idx(ns, nt)] > 0)) {
                        data[idx(ns, nt)] |= 0x04;
                        added = true;
                    }
                }
            }
        }

        if (added) {
            // Convert previous added to current points
            for (size_t i = 0; i < (source_length * target_length); ++i) {
                if (data[i] & 0x04)
                    data[i] = 0x03;
            }
        }
    }

    for (size_t i = 0; i < (source_length * target_length); ++i) {
        data[i] = (uint8_t) ((data[i] & 0x02) > 0 ? 1 : 0);
    }

    if (final) {
        //TODO: todo
    }

    // http://www.statmt.org/moses/?n=FactoredTraining.AlignWords
//    GROW-DIAG-FINAL(e2f,f2e):
//      neighboring = ((-1,0),(0,-1),(1,0),(0,1),(-1,-1),(-1,1),(1,-1),(1,1))
//      alignment = intersect(e2f,f2e);
//      GROW-DIAG(); FINAL(e2f); FINAL(f2e);
//
//     GROW-DIAG():
//      iterate until no new points added
//        for english word e = 0 ... en
//          for foreign word f = 0 ... fn
//            if ( e aligned with f )
//              for each neighboring point ( e-new, f-new ):
//                if ( ( e-new not aligned or f-new not aligned ) and
//                     ( e-new, f-new ) in union( e2f, f2e ) )
//                  add alignment point ( e-new, f-new )
//     FINAL(a):
//      for english word e-new = 0 ... en
//        for foreign word f-new = 0 ... fn
//          if ( ( e-new not aligned or f-new not aligned ) and
//               ( e-new, f-new ) in alignment a )
//            add alignment point ( e-new, f-new )
}

alignment_t SymAlignment::ToAlignment() {
    alignment_t alignment;

    for (size_t s = 0; s < source_length; ++s) {
        for (size_t t = 0; t < target_length; ++t) {
            if (data[idx(s, t)] > 0)
                alignment.push_back(pair<size_t, size_t>(s, t));
        }
    }

    return alignment;
}
