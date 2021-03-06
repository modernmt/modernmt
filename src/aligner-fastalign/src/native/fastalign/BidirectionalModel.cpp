//
// Created by Davide  Caroselli on 08/05/17.
//

#include "BidirectionalModel.h"

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

BidirectionalModel::BidirectionalModel(shared_ptr<bitable_t> table, bool forward, bool use_null,
                                       bool favor_diagonal, double prob_align_null, double diagonal_tension)
        : Model(!forward, use_null, favor_diagonal, prob_align_null, diagonal_tension), table(table) {
}

void BidirectionalModel::Open(istream &in, Model **outForward, Model **outBackward) {
    bool use_null;
    bool favor_diagonal;
    double prob_align_null;
    double fwd_diagonal_tension, bwd_diagonal_tension;

    in.read((char *) &use_null, sizeof(bool));
    in.read((char *) &favor_diagonal, sizeof(bool));

    in.read((char *) &prob_align_null, sizeof(double));
    in.read((char *) &fwd_diagonal_tension, sizeof(double));
    in.read((char *) &bwd_diagonal_tension, sizeof(double));

    shared_ptr<bitable_t> table(new bitable_t);

    size_t ttable_size;
    in.read((char *) &ttable_size, sizeof(size_t));

    table->resize(ttable_size);

    while (true) {
        word_t sourceWord;
        in.read((char *) &sourceWord, sizeof(word_t));

        if (in.eof())
            break;

        size_t row_size;
        in.read((char *) &row_size, sizeof(size_t));

        unordered_map<word_t, pair<float, float>> &row = table->at(sourceWord);
        row.reserve(row_size);

        for (size_t i = 0; i < row_size; ++i) {
            word_t targetWord;
            float first, second;

            in.read((char *) &targetWord, sizeof(word_t));
            in.read((char *) &first, sizeof(float));
            in.read((char *) &second, sizeof(float));

            row[targetWord] = pair<float, float>(first, second);
        }
    }

    *outForward = new BidirectionalModel(table, true, use_null, favor_diagonal, prob_align_null, fwd_diagonal_tension);
    *outBackward = new BidirectionalModel(table, false, use_null, favor_diagonal, prob_align_null,
                                          bwd_diagonal_tension);
}
