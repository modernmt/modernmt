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

    if (forward == nullptr)
        throw invalid_argument("Forward model is not specified!");


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
        auto &row = table[sourceWord];
        size_t row_size = row.size();

        if (row_size == 0)
            continue;

        out.write((const char *) &sourceWord, sizeof(word_t));
        out.write((const char *) &row_size, sizeof(size_t));

        for (auto it = row.begin(); it != row.end(); ++it) {
            out.write((const char *) &it->first, sizeof(word_t));
            out.write((const char *) &it->second.first, sizeof(float));
            out.write((const char *) &it->second.second, sizeof(float));
        }
    }
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

        auto &row = table->at(sourceWord);
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
        const auto &row = table->at(sid);
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

float BidirectionalModel::GetProbability(word_t source, word_t target) {
    if (is_reverse)
        std::swap(source, target);

    if (table->empty())
        return kNullProbability;
    if (source >= table->size())
        return kNullProbability;

    auto &row = table->at(source);
    auto ptr = row.find(target);
    return ptr == row.end() ? kNullProbability : (is_reverse ? ptr->second.second : ptr->second.first);
}