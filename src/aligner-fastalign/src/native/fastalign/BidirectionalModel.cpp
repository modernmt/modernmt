//
// Created by Davide  Caroselli on 08/05/17.
//

#include <assert.h>
#include <fstream>

#include "BidirectionalModel.h"

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

BidirectionalModel::BidirectionalModel(shared_ptr<bitable_t> table, bool forward, bool use_null,
                                       bool favor_diagonal, double prob_align_null, double diagonal_tension)
        : Model(!forward, use_null, favor_diagonal, prob_align_null, diagonal_tension), table(table) {
}

void BidirectionalModel::Store(const BidirectionalModel *forward, const BidirectionalModel *backward,
                               const string &filename) {
    ofstream out(filename, ios::binary | ios::out);

    out.write((const char *) &forward->use_null, sizeof(bool));
    out.write((const char *) &forward->favor_diagonal, sizeof(bool));

    out.write((const char *) &forward->prob_align_null, sizeof(double));
    out.write((const char *) &forward->diagonal_tension, sizeof(double));
    if (backward != nullptr){
        out.write((const char *) &backward->diagonal_tension, sizeof(double));
    } else{
        out.write((const char *) &kNullProbability, sizeof(double));
    }

    bitable_t &table = *forward->table;

    size_t ttable_size = table.size();
    out.write((const char *) &ttable_size, sizeof(size_t));

    for (word_t sourceWord = 0; sourceWord < table.size(); ++sourceWord) {
        unordered_map<word_t, pair<float, float>> &row = table[sourceWord];
        size_t row_size = row.size();

        if (row_size == 0)
            continue;

        out.write((const char *) &sourceWord, sizeof(word_t));
        out.write((const char *) &row_size, sizeof(size_t));

        for (auto it = row.begin(); it != row.end(); ++it) {
            word_t targetWord = it->first;

            out.write((const char *) &targetWord, sizeof(word_t));
            out.write((const char *) &it->second.first, sizeof(float));
            out.write((const char *) &it->second.second, sizeof(float));
        }
    }
}

void BidirectionalModel::Store(const string &fwd_path, const string &bwd_path, const string &out_path) {
    bool fwd_use_null, bwd_use_null;
    bool fwd_favor_diagonal, bwd_favor_diagonal;
    double fwd_prob_align_null, bwd_prob_align_null;
    double fwd_diagonal_tension, bwd_diagonal_tension;
    double dummy_double;
    float dummy_float;

    //opening forward and backward model files for reading
    //opening bidirectional model files for writing
    ifstream fwd_in(fwd_path, ios::binary | ios::in);
    ifstream bwd_in(bwd_path, ios::binary | ios::in);
    ofstream out(out_path, ios::binary | ios::out);

    //reading forward and backward model files in parallel
    //and writing into out model file
    fwd_in.read((char *) &fwd_use_null, sizeof(bool));
    bwd_in.read((char *) &bwd_use_null, sizeof(bool));
    assert(fwd_use_null == bwd_use_null);
    out.write((const char *) &fwd_use_null, sizeof(bool));

    fwd_in.read((char *) &fwd_favor_diagonal, sizeof(bool));
    bwd_in.read((char *) &bwd_favor_diagonal, sizeof(bool));
    assert(fwd_favor_diagonal == bwd_favor_diagonal);
    out.write((const char *) &fwd_favor_diagonal, sizeof(bool));

    fwd_in.read((char *) &fwd_prob_align_null, sizeof(double));
    bwd_in.read((char *) &bwd_prob_align_null, sizeof(double));
    assert(fwd_prob_align_null == bwd_prob_align_null);
    out.write((const char *) &fwd_prob_align_null, sizeof(double));

    fwd_in.read((char *) &fwd_diagonal_tension, sizeof(double));
    bwd_in.read((char *) &dummy_double, sizeof(double));
    out.write((const char *) &fwd_diagonal_tension, sizeof(double));

    fwd_in.read((char *) &dummy_double, sizeof(double));
    bwd_in.read((char *) &bwd_diagonal_tension, sizeof(double));
    out.write((const char *) &bwd_diagonal_tension, sizeof(double));

    size_t fwd_ttable_size, bwd_ttable_size;
    bwd_in.read((char *) &fwd_ttable_size, sizeof(size_t));
    bwd_in.read((char *) &bwd_ttable_size, sizeof(size_t));
    assert(fwd_ttable_size == bwd_ttable_size);
    out.write((const char *) &fwd_ttable_size, sizeof(size_t));

    while (true) {
        word_t fwd_sourceWord, bwd_sourceWord;
        fwd_in.read((char *) &fwd_sourceWord, sizeof(word_t));
        bwd_in.read((char *) &bwd_sourceWord, sizeof(word_t));

        if (fwd_in.eof() || bwd_in.eof())
            break;

        assert(fwd_sourceWord == bwd_sourceWord);
        out.write((const char *) &fwd_sourceWord, sizeof(word_t));


        size_t fwd_row_size, bwd_row_size;
        fwd_in.read((char *) &fwd_row_size, sizeof(size_t));
        bwd_in.read((char *) &bwd_row_size, sizeof(size_t));
        assert(fwd_row_size == bwd_row_size);
        out.write((const char *) &fwd_row_size, sizeof(size_t));

        //the order of fw_row and bwd_row can diffwr because of its type (unordered_map<word_t, pair<double, double>>)
        //hence, read them independently, then merge the bwd_row into the fwd_row, and then write the fwd_row to the model file
        unordered_map<word_t, pair<double, double>> fwd_row(fwd_row_size);

        for (size_t i = 0; i < fwd_row_size; ++i) {
            word_t fwd_targetWord;
            float first;

            fwd_in.read((char *) &fwd_targetWord, sizeof(word_t));
            fwd_in.read((char *) &first, sizeof(float));
            fwd_in.read((char *) &dummy_float, sizeof(float));
            fwd_row[fwd_targetWord] = pair<float, float>(first, kNullProbability);
        }

        for (size_t i = 0; i < fwd_row_size; ++i) {
            word_t bwd_targetWord;
            float second;

            bwd_in.read((char *) &bwd_targetWord, sizeof(word_t));
            bwd_in.read((char *) &dummy_float, sizeof(float));
            bwd_in.read((char *) &second, sizeof(float));

            out.write((const char *) &bwd_targetWord, sizeof(word_t));
            out.write((const char *) &fwd_row[bwd_targetWord].first, sizeof(float));
            out.write((const char *) &second, sizeof(float));
        }
    }

    fwd_in.close();
    bwd_in.close();
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
