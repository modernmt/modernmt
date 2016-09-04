//
// Created by Davide Caroselli on 04/09/16.
//

#include <iostream>
#include <getopt.h>
#include <fastalign/Corpus.h>
#include <fastalign/FastAligner.h>

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

string source_input;
string target_input;
string model_path;
int threads = 0;

struct option options[] = {
        {"source",  required_argument, NULL, 0},
        {"target",  required_argument, NULL, 0},
        {"model",   required_argument, NULL, 0},
        {"threads", optional_argument, NULL, 0},
        {0, 0, 0,                            0}
};

void help(const char *name) {
    cerr << "Usage: " << name << " -s file.fr -t file.en -m model\n"
         << "  -s: [REQ] Input source corpus\n"
         << "  -t: [REQ] Input target corpus\n"
         << "  -m: [REQ] Model path\n"
         << "  -n: Number of threads. (default = number of CPUs)\n";
}

bool InitCommandLine(int argc, char **argv) {
    while (true) {
        int oi;
        int c = getopt_long(argc, argv, "s:t:m:n:", options, &oi);
        if (c == -1) break;

        switch (c) {
            case 's':
                source_input = optarg;
                break;
            case 't':
                target_input = optarg;
                break;
            case 'm':
                model_path = optarg;
                break;
            case 'n':
                threads = atoi(optarg);
                break;
            default:
                return false;
        }
    }

    return !source_input.empty() && !target_input.empty() && !model_path.empty();
}

void printAlignment(vector<alignment_t> &alignments) {
    for (auto a = alignments.begin(); a != alignments.end(); ++a) {
        for (size_t i = 0; i < a->size(); ++i) {
            if (i > 0)
                cout << ' ';
            cout << a->at(i).first << '-' << a->at(i).second;
        }

        cout << endl;
    }
}

int main(int argc, char **argv) {
    if (!InitCommandLine(argc, argv)) {
        help(argv[0]);
        return 1;
    }

    Corpus corpus(source_input, target_input);
    CorpusReader reader(corpus);

    FastAligner *aligner = FastAligner::Open(model_path, threads);

    vector<pair<vector<wid_t>, vector<wid_t>>> batch;
    vector<alignment_t> alignments;

    while (reader.Read(batch, 10000)) {
        aligner->GetAlignments(batch, alignments, GrowDiagonalStrategy);

        printAlignment(alignments);

        alignments.clear();
        batch.clear();
    }

    delete aligner;
}