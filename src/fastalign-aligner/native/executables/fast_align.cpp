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
#include <sys/time.h>

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

string source_input;
string target_input;
string model_path;

Options builderOptions(false);

struct option options[] = {
        {"source",     required_argument, NULL, 0},
        {"target",     required_argument, NULL, 0},
        {"model",      required_argument, NULL, 0},
        {"threads",    optional_argument, NULL, 0},
        {"reverse",    no_argument,       NULL, 0},
        {"iterations", optional_argument, NULL, 0},
        {0, 0, 0,                               0}
};

void help(const char *name) {
    cerr << "Usage: " << name << " -s file.fr -t file.en -m model\n"
         << "  -s: [REQ] Input source corpus\n"
         << "  -t: [REQ] Input target corpus\n"
         << "  -m: [REQ] Output model path\n"
         << "  -r: Run alignment in reverse (condition on target and predict source)\n"
         << "  -I: number of iterations in EM training (default = 5)\n"
         << "  -n: Number of threads. (default = number of CPUs)\n";
}

bool InitCommandLine(int argc, char **argv) {
    while (true) {
        int oi;
        int c = getopt_long(argc, argv, "s:t:m:rI:n:", options, &oi);
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
            case 'r':
                builderOptions.is_reverse = true;
                break;
            case 'I':
                builderOptions.iterations = atoi(optarg);
                break;
            case 'n':
                builderOptions.threads = atoi(optarg);
                break;
            default:
                return false;
        }
    }

    return !source_input.empty() && !target_input.empty() && !model_path.empty();
}

class ProcessListener : public ModelBuilder::Listener {
public:
    virtual void Begin() override {
        processBegin = GetTime();
    }

    virtual void IterationBegin(int iteration) override {
        cerr << "Iteration " << iteration << ":" << endl;
    }

    virtual void Begin(const BuilderStep step, int iteration) override {
        if (iteration > 0)
            cerr << "\t";

        string str_step;
        switch (step) {
            case kBuilderStepSetup:
                str_step = "Initial setup";
                break;
            case kBuilderStepAligning:
                str_step = "Computing alignments";
                break;
            case kBuilderStepOptimizingDiagonalTension:
                str_step = "Optimizing diagonal tension";
                break;
            case kBuilderStepNormalizing:
                str_step = "Normalizing translation table";
                break;
            case kBuilderStepPruning:
                str_step = "Pruning model";
                break;
            case kBuilderStepStoringModel:
                str_step = "Storing model";
                break;
            default:
                str_step = "Unknown step";
                break;
        }

        cerr << str_step << "... ";
        stepBegin = GetTime();
    }

    virtual void End(const BuilderStep step, int iteration) override {
        cerr << "DONE in " << (GetTime() - stepBegin) << "s" << endl;
    }

    virtual void IterationEnd(int iteration) override {
        // Nothing to do
    }

    virtual void End() override {
        cerr << "\nTraining done in " << (GetTime() - processBegin) << "s" << endl;

    }

private:
    double stepBegin;
    double processBegin;

    double GetTime() {
        struct timeval time;

        if (gettimeofday(&time, NULL)) {
            //  Handle error
            return 0;
        }

        return (double) time.tv_sec + ((double) time.tv_usec / 1000000.);
    }
};

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

void print(const Corpus &corpus, Model *model, size_t buffer_size) {
    CorpusReader reader(corpus);
    vector<pair<string, string>> batch;
    vector<alignment_t> alignments;

    while (reader.ReadLines(batch, buffer_size)) {
        model->ComputeAlignments(batch, alignments);

        printAlignment(alignments);

        alignments.clear();
        batch.clear();
    }
}

int main(int argc, char **argv) {
    if (!InitCommandLine(argc, argv)) {
        help(argv[0]);
        return 1;
    }

    ProcessListener listener;
    Corpus corpus(source_input, target_input);

    ModelBuilder builder(builderOptions);
    builder.setListener(&listener);

    Model *model = builder.Build(corpus, model_path);

    print(corpus, model, builderOptions.buffer_size);
}
