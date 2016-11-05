//
// Created by Nicola Bertoldi on 05/11/16.
//

#ifndef SAPT_DISTORTIONCOUNTS_H
#define SAPT_DISTORTIONCOUNTS_H

#include <cassert>

#include <iostream>

namespace mmt {
    namespace sapt {

        //constants for the reordering statistics
        const size_t kTranslationOptionDistortionCount = 4;  //todo: this number should be read from outside because it has to match the lexicalized reordering.
        const size_t kTOMonotonicOrientation = 0;
        const size_t kTONonMonotonicOrientation = 1;
        const size_t kTOSwapOrientation = 1;
        const size_t kTODiscontinuousOrientation = 2;
        const size_t kTODiscontinuousLeftOrientation = 2;
        const size_t kTODiscontinuousRightOrientation = 3;
        const size_t kTORightOrientation = 0;
        const size_t kTOLeftOrientation = 1;
        const size_t kTOMaxOrientation = 3;
        const size_t kTONoneOrientation = 4;

        const size_t po_other = kTONoneOrientation;

        struct OrientationType {

// check if min and max in the alignment vector v are within the
// bounds LFT and RGT and update the actual bounds L and R; update
// the total count of alignment links in the underlying phrase
// pair
            static bool
            check(vector<ushort> const &v, // alignment row/column
                  size_t const LFT, size_t const RGT, // hard limits
                  ushort &L, ushort &R, size_t &count) // current bounds, count
            {
                if (v.size() == 0) return 0;
                if (L > v.front() && (L = v.front()) < LFT) return false;
                if (R < v.back() && (R = v.back()) > RGT) return false;
                count += v.size();
                return true;
            }

/// return number of alignment points in box, -1 on failure
            static int
            expand_block(vector<vector<ushort> > const &row2col,
                         vector<vector<ushort> > const &col2row,
                         size_t row, size_t col, // seed coordinates
                         size_t const TOP, size_t const LFT, // hard limits
                         size_t const BOT, size_t const RGT, // hard limits
                         ushort *top = NULL, ushort *lft = NULL,
                         ushort *bot = NULL, ushort *rgt = NULL) // store results
            {
                if (row < TOP || row > BOT || col < LFT || col > RGT) return -1;
                assert(row < row2col.size());
                assert(col < col2row.size());

                // ====================================================
                // tables grow downwards, so TOP is smaller than BOT!
                // ====================================================

                ushort T, L, B, R; // box dimensions

                // if we start on an empty cell, search for the first alignment point
                if (row2col[row].size() == 0 && col2row[col].size() == 0) {
                    if (row == TOP) while (row < BOT && !row2col[++row].size());
                    else if (row == BOT) while (row > TOP && !row2col[--row].size());

                    if (col == LFT) while (col < RGT && !col2row[++col].size());
                    else if (col == RGT) while (col > RGT && !col2row[--col].size());

                    if (row2col[row].size() == 0 && col2row[col].size() == 0)
                        return 0;
                }
                if (row2col[row].size() == 0)
                    row = col2row[col].front();
                if (col2row[col].size() == 0)
                    col = row2col[row].front();

                if ((T = col2row[col].front()) < TOP) return -1;
                if ((B = col2row[col].back()) > BOT) return -1;
                if ((L = row2col[row].front()) < LFT) return -1;
                if ((R = row2col[row].back()) > RGT) return -1;

                if (B == T && R == L) return 1;

                // start/end of row / column coverage:
                ushort rs = row, re = row, cs = col, ce = col;
                int ret = (int) row2col[row].size();
                for (size_t tmp = 1; tmp; ret += tmp) {
                    tmp = 0;
                    while (rs > T) if (!check(row2col[--rs], LFT, RGT, L, R, tmp)) return -1;
                    while (re < B) if (!check(row2col[++re], LFT, RGT, L, R, tmp)) return -1;
                    while (cs > L) if (!check(col2row[--cs], TOP, BOT, T, B, tmp)) return -1;
                    while (ce < R) if (!check(col2row[++ce], TOP, BOT, T, B, tmp)) return -1;
                }
                if (top) *top = T;
                if (bot) *bot = B;
                if (lft) *lft = L;
                if (rgt) *rgt = R;
                return ret;
            }

            static size_t
            find_po_fwd(vector<vector<ushort> > &a1,
                        vector<vector<ushort> > &a2,
                        size_t s1, size_t e1,
                        size_t s2, size_t e2) {
                if (e2 == a2.size()) { // end of target sentence
                    return mmt::sapt::kTOMonotonicOrientation;
                }
                size_t y = e2, L = e2, R = a2.size() - 1; // won't change
                size_t x = e1, T = e1, B = a1.size() - 1;
                if (e1 < a1.size() && expand_block(a1, a2, x, y, T, L, B, R) >= 0) {
                    return mmt::sapt::kTOMonotonicOrientation;
                }
                B = x = s1 - 1;
                T = 0;
                if (s1 && expand_block(a1, a2, x, y, T, L, B, R) >= 0) {
                    return mmt::sapt::kTOSwapOrientation;
                }
                while (e2 < a2.size() && a2[e2].size() == 0) ++e2;
                if (e2 == a2.size()) { // should never happen, actually
                    return mmt::sapt::kTONoneOrientation;
                }
                if (a2[e2].back() < s1) {
                    return mmt::sapt::kTODiscontinuousLeftOrientation;
                }
                if (a2[e2].front() >= e1) {
                    return mmt::sapt::kTODiscontinuousRightOrientation;
                }
                return mmt::sapt::kTONoneOrientation;
            }

            static size_t
            find_po_bwd(vector<vector<ushort> > &a1,
                        vector<vector<ushort> > &a2,
                        size_t s1, size_t e1,
                        size_t s2, size_t e2) {

                if (s1 == 0 && s2 == 0) {
                    return mmt::sapt::kTOMonotonicOrientation;
                }
                if (s2 == 0) {
                    return mmt::sapt::kTODiscontinuousRightOrientation;
                }
                if (s1 == 0) {
                    return mmt::sapt::kTODiscontinuousLeftOrientation;
                }
                size_t y = s2 - 1, L = 0, R = s2 - 1; // won't change
                size_t x = s1 - 1, T = 0, B = s1 - 1;
                if (expand_block(a1, a2, x, y, T, L, B, R) >= 0) {
                    return mmt::sapt::kTOMonotonicOrientation;
                }
                T = x = e1;
                B = a1.size() - 1;
                if (expand_block(a1, a2, x, y, T, L, B, R) >= 0) {
                    return mmt::sapt::kTOSwapOrientation;
                }
                while (s2-- && a2[s2].size() == 0);

                size_t ret;
                ret = (a2[s2].size() == 0 ? po_other :
                       a2[s2].back() < s1 ? mmt::sapt::kTODiscontinuousRightOrientation :
                       a2[s2].front() >= e1 ? mmt::sapt::kTODiscontinuousLeftOrientation :
                       po_other);
                return ret;
            }
        };

        struct OrientationCounts {
            vector<float> ForwardOrientationCounts; //counts or probs? integer or floats?
            vector<float> BackwardOrientationCounts; //counts or probs? integer or floats?

            OrientationCounts(){
                ForwardOrientationCounts.resize(kTranslationOptionDistortionCount + 1, 0);  // the vector include the entry for NONE
                BackwardOrientationCounts.resize(kTranslationOptionDistortionCount + 1, 0);  // the vector include the entry for NONE
            }

            OrientationCounts(const OrientationCounts &o){
                ForwardOrientationCounts = o.ForwardOrientationCounts;
                BackwardOrientationCounts = o.BackwardOrientationCounts;
            }

            void Add(size_t fwd, size_t bwd){
                ForwardOrientationCounts[fwd]++;
                BackwardOrientationCounts[bwd]++;
            }
        };
    }
}
#endif //SAPT_DISTORTIONCOUNTS_H
