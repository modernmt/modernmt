// Copyright 2013 by Chris Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

#include <iostream>
#include <getopt.h>
#include <stdlib.h>
#include <fastalign/ModelBuilder.h>
#include <sstream>

using namespace std;
using namespace fastalign;

string input;
string conditional_probability_filename = "";
string input_model_file = "";
string StringForExit = "EXIT";
double mean_srclen_multiplier = 1.0;
int is_reverse = 0;
int ITERATIONS = 5;
int favor_diagonal = 0;
double beam_threshold = -4.0;
double prob_align_null = 0.08;
double diagonal_tension = 4.0;
int optimize_tension = 0;
int variational_bayes = 0;
double alpha = 0.01;
int no_null_word = 0;
size_t thread_buffer_size = 10000;
bool force_align = false;
int NumberOfThreads = 8;
struct option options[] = {
        {"input",                     required_argument, 0,                  'i'},
        {"reverse",                   no_argument,       &is_reverse,        1},
        {"iterations",                required_argument, 0,                  'I'},
        {"favor_diagonal",            no_argument,       &favor_diagonal,    1},
        {"force_align",               required_argument, 0,                  'f'},
        {"mean_srclen_multiplier",    required_argument, 0,                  'm'},
        {"beam_threshold",            required_argument, 0,                  't'},
        {"p0",                        required_argument, 0,                  'q'},
        {"diagonal_tension",          required_argument, 0,                  'T'},
        {"optimize_tension",          no_argument,       &optimize_tension,  1},
        {"variational_bayes",         no_argument,       &variational_bayes, 1},
        {"alpha",                     required_argument, 0,                  'a'},
        {"no_null_word",              no_argument,       &no_null_word,      1},
        {"conditional_probabilities", required_argument, 0,                  'p'},
        {"thread_buffer_size",        required_argument, 0,                  'b'},
        {"NumberOfThreads",           required_argument, 0,                  'n'},
        {"BinaryDump",                required_argument, 0,                  'B'},
        {"StringForExit",             required_argument, 0,                  'e'},
        {0, 0,                                           0,                  0}
};

void help(const char *name) {
    cerr << "Usage: " << name << " -i file.fr-en\n"
         << " Standard options ([USE] = strongly recommended):\n"
         << "  -i: [REQ] Input parallel corpus\n"
         << "  -v: [USE] Use Dirichlet prior on lexical translation distributions\n"
         << "  -d: [USE] Favor alignment points close to the monotonic diagonoal\n"
         << "  -o: [USE] Optimize how close to the diagonal alignment points should be\n"
         << "  -r: Run alignment in reverse (condition on target and predict source)\n"
         << "  -p: Output conditional probability table\n"
         << "  -f: Forced Align mode. Requires a conditional probability table to perform alignment.\n"
         << " Advanced options:\n"
         << "  -I: number of iterations in EM training (default = 5)\n"
         << "  -q: p_null parameter (default = 0.08)\n"
         << "  -N: No null word\n"
         << "  -m: Mean Source Length Multiplier.\n"
         << "  -n: Number of threads. (default = 8)\n"
         << "  -b: thread buffer size (default = 10000). [Use 0 to run in the single mode.]\n"
         << "  -a: alpha parameter for optional Dirichlet prior (default = 0.01)\n"
         << "  -T: starting lambda for diagonal distance parameter (default = 4)\n"
         << "  -B: Save and load the models in the Binary mode? (default = False)\n"
         << "  -e: string for exit (default = EXIT)\n";
}

bool InitCommandLine(int argc, char **argv) {
    while (1) {
        int oi;
        int c = getopt_long(argc, argv, "i:rI:df:m:t:q:T:ova:Np:b:n:Be:", options, &oi);
        if (c == -1) break;
        switch (c) {
            case 'i':
                input = optarg;
                break;
            case 'r':
                is_reverse = 1;
                break;
            case 'I':
                ITERATIONS = atoi(optarg);
                break;
            case 'd':
                favor_diagonal = 1;
                break;
            case 'f':
                force_align = 1;
                conditional_probability_filename = optarg;
                break;
            case 'm':
                mean_srclen_multiplier = atof(optarg);
                break;
            case 't':
                beam_threshold = atof(optarg);
                break;
            case 'q':
                prob_align_null = atof(optarg);
                break;
            case 'T':
                diagonal_tension = atof(optarg);
                break;
            case 'o':
                optimize_tension = 1;
                break;
            case 'v':
                variational_bayes = 1;
                break;
            case 'a':
                alpha = atof(optarg);
                break;
            case 'N':
                no_null_word = 1;
                break;
            case 'p':
                conditional_probability_filename = optarg;
                break;
            case 'n':
                NumberOfThreads = atoi(optarg);
                break;
            case 'b':
                thread_buffer_size = atoi(optarg);
                break;
            case 'e':
                StringForExit = optarg;
                break;
            default:
                return false;
        }
    }
    if (input.size() == 0) return false;
    return true;
}

int main(int argc, char **argv) {
    if (!InitCommandLine(argc, argv)) {
        help(argv[0]);
        return 1;
    }

    string source;
    string target;
    stringstream ss(input);

    getline(ss, source, ',');
    getline(ss, target, ',');

    Corpus corpus(source, target);

    Options builderOptions(is_reverse);
    ModelBuilder builder(builderOptions);

    builder.Build(corpus, conditional_probability_filename);

}
