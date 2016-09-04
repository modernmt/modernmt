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
                delete[] data;
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

            size_t size = 0;
            uint8_t *data = NULL;

            inline size_t idx(size_t s, size_t t) const {
                return s * target_length + t;
            }

            inline void Merge(const alignment_t &forward, const alignment_t &backward) {
                for (auto it = forward.begin(); it != forward.end(); ++it) {
                    data[idx(it->first, it->second)] |= 0x01;
                }

                for (auto it = backward.begin(); it != backward.end(); ++it) {
                    data[idx(it->first, it->second)] |= 0x02;
                }
            }

            inline bool IsSourceWordAligned(size_t s) const {
                s = s * target_length;

                for (size_t t = 0; t < target_length; ++t) {
                    if ((data[s + t] & 0x07) > 2)
                        return true;
                }

                return false;
            }

            bool IsTargetWordAligned(size_t t) const {
                for (size_t s = 0; s < source_length; ++s) {
                    size_t i = s * target_length + t;
                    if ((data[i] & 0x07) > 2)
                        return true;
                }

                return false;
            }
        };

    }
}


#endif //FASTALIGN_SYMMETRIZER_H
