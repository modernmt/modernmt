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

void BidirectionalModel::Open(const string &filename, Model **outForward, Model **outBackward) {
    bool use_null;
    bool favor_diagonal;
    double prob_align_null;
    double fwd_diagonal_tension, bwd_diagonal_tension;

    ifstream in(filename, ios::binary | ios::in);

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

void BidirectionalModel::ExportLexicalModel(const string &filename, const Vocabulary *vb) {
    ofstream out(filename, ios::binary | ios::out);

    for (word_t sid = 0; sid < table->size(); ++sid) {
        const unordered_map<word_t, pair<float, float>> &row = table->at(sid);
        size_t row_size = row.size();

        if (row_size == 0)
            continue;

        out << "<" << vb->Get(sid) << "> " << row_size << endl;

        for (auto it = row.begin(); it != row.end(); ++it) {
            word_t tid = it->first;
            out << "  <" << vb->Get(tid) << "> " << it->second.first << " " << it->second.second << endl;
        }
    }
}