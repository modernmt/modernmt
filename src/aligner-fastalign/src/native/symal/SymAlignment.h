//
// Created by Davide Caroselli on 04/09/16.
//

#ifndef FASTALIGN_SYMMETRIZER_H
#define FASTALIGN_SYMMETRIZER_H

#include <stddef.h>
#include <mmt/sentence.h>

namespace mmt {
    namespace fastalign {

        class SymAlignment {
        public:

            SymAlignment() {};

            SymAlignment(size_t source_length, size_t target_length) {
                Reset(source_length, target_length);
            }

            ~SymAlignment() {
                delete[] m;
                delete[] src_coverage;
                delete[] trg_coverage;
            }

            void Reset(size_t source_length, size_t target_length);

            void Union(const alignment_t &forward, const alignment_t &backward);

            void Intersection(const alignment_t &forward, const alignment_t &backward);

            void Grow(const alignment_t &forward, const alignment_t &backward, bool diagonal = true,
                             bool final = true);

            alignment_t ToAlignment();

        private:
            size_t source_length = 0;
            size_t target_length = 0;

            uint8_t *m = NULL;
            uint8_t *src_coverage = NULL;
            uint8_t *trg_coverage = NULL;

            size_t m_size = 0;
            size_t src_coverage_size = 0;
            size_t trg_coverage_size = 0;

            inline size_t idx(size_t s, size_t t) {
                return s * target_length + t;
            }

            inline void Merge(const alignment_t &forward, const alignment_t &backward) {
                for (auto it = forward.begin(); it != forward.end(); ++it)
                    m[idx(it->first, it->second)] |= 0x01;

                for (auto it = backward.begin(); it != backward.end(); ++it) {
                    size_t i = idx(it->first, it->second);

                    m[i] |= 0x02;

                    if ((m[i] & 0x03) == 0x03) {
                        src_coverage[it->first] = 1;
                        trg_coverage[it->second] = 1;
                    }
                }

            }


        };

    }
}


#endif //FASTALIGN_SYMMETRIZER_H
