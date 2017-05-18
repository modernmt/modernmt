//
// Created by Davide  Caroselli on 12/05/17.
//

#include "LexicalModel.h"
#include <fstream>
#include <boost/filesystem.hpp>

namespace fs = boost::filesystem;

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

LexicalModel::LexicalModel() {

}

LexicalModel::LexicalModel(const std::string &path) {
    fs::path filename = fs::absolute(fs::path(path) / fs::path("model.lex"));
    if (!fs::is_regular(filename))
        throw invalid_argument("File not found: " + filename.string());

    ifstream in(filename.string(), ios::binary | ios::in);

    size_t ttable_size;
    in.read((char *) &ttable_size, sizeof(size_t));

    model.resize(ttable_size);

    while (true) {
        wid_t sourceWord;
        in.read((char *) &sourceWord, sizeof(wid_t));

        if (in.eof())
            break;

        size_t row_size;
        in.read((char *) &row_size, sizeof(size_t));

        unordered_map<wid_t, pair<float, float>> &row = model[sourceWord];
        row.reserve(row_size);

        for (size_t i = 0; i < row_size; ++i) {
            wid_t targetWord;
            float forward, backward;

            in.read((char *) &targetWord, sizeof(wid_t));
            in.read((char *) &forward, sizeof(float));
            in.read((char *) &backward, sizeof(float));

            row[targetWord] = pair<float, float>(forward, backward);
        }
    }
}

void LexicalModel::Store(const std::string &filename) {
    ofstream out(filename, ios::binary | ios::out);

    size_t ttable_size = model.size();
    out.write((const char *) &ttable_size, sizeof(size_t));

    for (wid_t sourceWord = 0; sourceWord < model.size(); ++sourceWord) {
        unordered_map<wid_t, pair<float, float>> &row = model[sourceWord];
        size_t row_size = row.size();

        if (row_size == 0)
            continue;

        out.write((const char *) &sourceWord, sizeof(wid_t));
        out.write((const char *) &row_size, sizeof(size_t));

        for (auto it = row.begin(); it != row.end(); ++it) {
            wid_t targetWord = it->first;
            float forward = it->second.first;
            float backward = it->second.second;

            out.write((const char *) &targetWord, sizeof(wid_t));
            out.write((const char *) &forward, sizeof(float));
            out.write((const char *) &backward, sizeof(float));
        }
    }
}

inline wid_t GetWordId(Vocabulary &vb, const std::string &word) {
    if (word.size() > 2) {
        wid_t id = vb.Lookup(word.substr(1, word.size() - 2), false);
        if (id == kVocabularyUnknownWord)
            throw invalid_argument("Unknown word: " + word);

        return id;
    } else {
        return LexicalModel::kNullWord;
    }
}

LexicalModel *LexicalModel::Import(Vocabulary &vb, const std::string &path) {
    LexicalModel *lex = new LexicalModel();

    fstream in(path, ios::binary | ios::in);
    lex->model.resize(100000);

    while (!in.eof()) {
        string sourceWord;
        size_t row_size;

        if (!(in >> sourceWord))
            break;
        if (!(in >> row_size))
            break;

        wid_t sourceWordId = GetWordId(vb, sourceWord);

        if (sourceWordId >= lex->model.size())
            lex->model.resize(sourceWordId + 1);

        unordered_map<wid_t, pair<float, float>> &row = lex->model[sourceWordId];
        row.reserve(row_size);

        for (size_t j = 0; j < row_size; ++j) {
            string targetWord;
            float forward, backward;

            in >> targetWord >> forward >> backward;

            row[GetWordId(vb, targetWord)] = pair<float, float>(forward, backward);
        }
    }

    // Trim
    size_t lastIndex;
    for (lastIndex = lex->model.size() - 1; lastIndex > 0; --lastIndex) {
        if (!lex->model[lastIndex].empty())
            break;
    }
    lex->model.resize(lastIndex + 1);

    return lex;
}