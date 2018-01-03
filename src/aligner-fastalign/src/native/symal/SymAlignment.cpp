//
// Created by Davide Caroselli on 04/09/16.
//

#include "SymAlignment.h"
#include <stdlib.h>
#include <cstring>

using namespace std;
using namespace mmt;
using namespace fastalign;

#define IsInIntersection(a) (((a) & 0x03) == 0x03)
#define IsInUnion(a) (((a) & 0x03) > 0)
#define HasBeenAdded(a) (((a) & 0x04) == 0x04)
#define IsInForward(a) (((a) & 0x01) > 0)
#define IsInBackward(a) (((a) & 0x02) > 0)

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
    score = 0;
    source_length = _source_length;
    target_length = _target_length;

    size_t m_size_ = source_length * target_length;
    if (m_size_ > m_size) {
        m_size = m_size_;
        m = (uint8_t *) realloc(m, m_size);
    }

    if (source_length > src_coverage_size) {
        src_coverage_size = source_length;
        src_coverage = (uint8_t *) realloc(src_coverage, src_coverage_size);
    }

    if (target_length > trg_coverage_size) {
        trg_coverage_size = target_length;
        trg_coverage = (uint8_t *) realloc(trg_coverage, trg_coverage_size);
    }

    memset(m, 0, m_size);
    memset(src_coverage, 0, src_coverage_size);
    memset(trg_coverage, 0, trg_coverage_size);
}

void SymAlignment::Union(const alignment_t &forward, const alignment_t &backward) {
    Merge(forward, backward);
}

void SymAlignment::Intersection(const alignment_t &forward, const alignment_t &backward) {
    Merge(forward, backward);

    for (size_t i = 0; i < (source_length * target_length); ++i) {
        m[i] = (uint8_t) (IsInIntersection(m[i]) ? 1 : 0);
    }
}

void SymAlignment::Grow(const alignment_t &forward, const alignment_t &backward, bool diagonal, bool final) {
    Merge(forward, backward);

    size_t neighbors_size = diagonal ? 8 : 4;

    bool added = true;
    while (added) {
        added = false;

        for (size_t t = 0; t < target_length; ++t) {
            for (size_t s = 0; s < source_length; ++s) {
                uint8_t point = m[idx(s, t)];

                if (IsInIntersection(point) || HasBeenAdded(point)) {
                    for (size_t ni = 0; ni < neighbors_size; ++ni) {
                        size_t ns = s + kGrowDiagonalNeighbors[ni][0];
                        size_t nt = t + kGrowDiagonalNeighbors[ni][1];

                        if (ns >= source_length || nt >= target_length)
                            continue; // point is outside matrix

                        if (!(src_coverage[ns] && trg_coverage[nt]) && IsInUnion(m[idx(ns, nt)])) {
                            m[idx(ns, nt)] |= 0x04;
                            src_coverage[ns] = 1;
                            trg_coverage[nt] = 1;
                            added = true;
                        }
                    }
                }
            }
        }
    }

    if (final) {
        // Forward Final-And
        for (size_t t = 0; t < target_length; ++t) {
            for (size_t s = 0; s < source_length; ++s) {
                if (IsInForward(m[idx(s, t)]) && !(src_coverage[s] || trg_coverage[t])) {
                    m[idx(s, t)] |= 0x04;
                    src_coverage[s] = 1;
                    trg_coverage[t] = 1;
                }
            }
        }

        // Forward Final-And
        for (size_t t = 0; t < target_length; ++t) {
            for (size_t s = 0; s < source_length; ++s) {
                if (IsInBackward(m[idx(s, t)]) && !(src_coverage[s] || trg_coverage[t])) {
                    m[idx(s, t)] |= 0x04;
                    src_coverage[s] = 1;
                    trg_coverage[t] = 1;
                }
            }
        }
    }

    for (size_t i = 0; i < (source_length * target_length); ++i)
        m[i] = (uint8_t) (IsInIntersection(m[i]) || HasBeenAdded(m[i]) ? 1 : 0);
}

alignment_t SymAlignment::ToAlignment() {
    alignment_t alignment;
    alignment.score = score;

    for (size_t s = 0; s < source_length; ++s) {
        for (size_t t = 0; t < target_length; ++t) {
            if (m[idx(s, t)] > 0)
                alignment.points.push_back(pair<size_t, size_t>(s, t));
        }
    }

    return alignment;
}
