//
// Created by Davide  Caroselli on 16/08/16.
//

#include <vocabulary/PersistentVocabulary.h>
#include <sstream>
#include <iostream>
#include <fstream>

using namespace std;

const size_t kBufferSizeInWords = 20000000;

void WhitespaceTokenize(string &line, vector<uint32_t> &output) {
    istringstream iss(line);
    string word;

    while (getline(iss, word, ' ')) {
        output.push_back((uint32_t &&) stoi(word));
    }
}

void FlushToStdout(vector<vector<string>> &output) {
    for (auto it = output.begin(); it != output.end(); ++it) {
        for (size_t i = 0; i < it->size(); ++i) {
            if (i > 0)
                cout << ' ';

            cout << it->at(i);
        }

        cout << '\n';
    }
}

int main(int argc, const char *argv[]) {
    if (argc != 2) {
        cerr << "USAGE: vbdecode <model_path>" << endl;
        exit(1);
    }

    string modelPath = argv[1];

    PersistentVocabulary vocabulary(modelPath);

    vector<vector<uint32_t>> buffer;
    vector<vector<string>> output;

    buffer.reserve(kBufferSizeInWords / 20);
    output.reserve(kBufferSizeInWords / 20);

    size_t bufferSizeInWords = 0;

    for (string line; getline(cin, line);) {
        vector<uint32_t > tokens;
        WhitespaceTokenize(line, tokens);
        buffer.push_back(tokens);
        bufferSizeInWords += tokens.size();

        if (bufferSizeInWords >= kBufferSizeInWords) {
            vocabulary.ReverseLookup(buffer, output);
            FlushToStdout(output);

            buffer.clear();
            output.clear();
            bufferSizeInWords = 0;
        }
    }

    if (buffer.size() > 0) {
        vocabulary.ReverseLookup(buffer, output);
        FlushToStdout(output);
    }

    return 0;
}
