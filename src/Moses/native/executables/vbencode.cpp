//
// Created by Davide Caroselli on 26/07/16.
//

#include <vocabulary/Vocabulary.h>
#include <sstream>
#include <iostream>

using namespace std;

const size_t kBufferSize = 1000000;

void WhitespaceTokenize(string &line, vector<string> &output) {
    istringstream iss(line);
    string word;

    while (getline(iss, word, ' ')) {
        output.push_back(word);
    }
}

void FlushToStdout(vector<vector<uint32_t>> &output) {
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
        cerr << "USAGE: vbencode <model_path>" << endl;
        exit(1);
    }

    string modelPath = argv[1];
    Vocabulary vocabulary(modelPath);

    vector<vector<string>> buffer;
    vector<vector<uint32_t>> output;

    buffer.reserve(kBufferSize);
    output.reserve(kBufferSize);

    for (string line; getline(cin, line);) {
        vector<string> tokens;
        WhitespaceTokenize(line, tokens);
        buffer.push_back(tokens);

        if (buffer.size() >= kBufferSize) {
            vocabulary.Lookup(buffer, output, false);

            FlushToStdout(output);

            buffer.clear();
            output.clear();
        }
    }

    if (buffer.size() > 0) {
        vocabulary.Lookup(buffer, output, false);
        FlushToStdout(output);
    }

    return 0;
}