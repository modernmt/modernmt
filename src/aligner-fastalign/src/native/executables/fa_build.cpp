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
#include <fastalign/Builder.h>
#include <sys/time.h>
#include <fastalign/FastAligner.h>

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

string source_lang;
string target_lang;
string input_path;
string model_path;

Options builderOptions;

struct option options[] = {
        {"source",         required_argument, NULL, 0},
        {"target",         required_argument, NULL, 0},
        {"input",          required_argument, NULL, 0},
        {"model",          required_argument, NULL, 0},
        {"threads",        optional_argument, NULL, 0},
        {"iterations",     optional_argument, NULL, 0},
        {"pruning",        optional_argument, NULL, 0},
        {"length",         optional_argument, NULL, 0},
        {"case_sensitive", optional_argument, NULL, 0},
        {0, 0, 0,                                   0}
};

void help(const char *name) {
    cerr << "Usage: " << name << " -s file.fr -t file.en -m model\n"
         << "  -s: [REQ] Source language\n"
         << "  -t: [REQ] Target language\n"
         << "  -i: [REQ] Input path with training corpora\n"
         << "  -m: [REQ] Output model path\n"
         << "  -I: number of iterations in EM training (default = 5)\n"
         << "  -n: Number of threads. (default = number of CPUs)\n"
         << "  -p: Pruning threshold. (default = 1.e-20)\n"
         << "  -l: Max sentence length. (default = 80)\n"
         << "  -c: create a case-insensitive model (default is case sensitive)\n";
}

bool InitCommandLine(int argc, char **argv) {
    while (true) {
        int oi;
        int c = getopt_long(argc, argv, "s:t:i:m:I:n:p:l:c", options, &oi);
        if (c == -1) break;

        switch (c) {
            case 's':
                source_lang = optarg;
                break;
            case 't':
                target_lang = optarg;
                break;
            case 'i':
                input_path = optarg;
                break;
            case 'm':
                model_path = optarg;
                break;
            case 'I':
                builderOptions.iterations = atoi(optarg);
                break;
            case 'n':
                builderOptions.threads = atoi(optarg);
                break;
            case 'p':
                builderOptions.pruning_threshold = atof(optarg);
                break;
            case 'l':
                builderOptions.max_line_length = static_cast<size_t>(atoi(optarg));
                break;
            case 'c':
                builderOptions.case_sensitive = false;
                break;
            default:
                return false;
        }
    }

    return !source_lang.empty() && !target_lang.empty() && !input_path.empty() && !model_path.empty();
}

class ProcessListener : public Builder::Listener {
public:
    void VocabularyBuildBegin() override {
        cerr << "Creating vocabulary... ";
        stepBegin = GetTime();
    }

    void VocabularyBuildEnd() override {
        cerr << "DONE in " << (GetTime() - stepBegin) << "s" << endl;
    }

    virtual void Begin(bool forward) override {
        processBegin = GetTime();
        cerr << "== " << (forward ? "Forward" : "Backward") << " model training ==" << endl;
    }

    virtual void IterationBegin(bool forward, int iteration) override {
        cerr << "Iteration " << iteration << ":" << endl;
    }

    virtual void Begin(bool forward, const BuilderStep step, int iteration) override {
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
            default:
                str_step = "Unknown step";
                break;
        }

        cerr << str_step << "... ";
        stepBegin = GetTime();
    }

    virtual void End(bool forward, const BuilderStep step, int iteration) override {
        cerr << "DONE in " << (GetTime() - stepBegin) << "s" << endl;
    }

    virtual void IterationEnd(bool forward, int iteration) override {
        // Nothing to do
    }

    virtual void End(bool forward) override {
        cerr << "\nTraining DONE in " << (GetTime() - processBegin) << "s" << endl << endl;
    }

    void ModelDumpBegin() override {
        cerr << "Writing model...";
        stepBegin = GetTime();
    }

    void ModelDumpEnd() override {
        cerr << "DONE in " << (GetTime() - stepBegin) << "s" << endl;
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

int main(int argc, char **argv) {
    if (!InitCommandLine(argc, argv)) {
        help(argv[0]);
        return 1;
    }

    ProcessListener listener;
    vector<Corpus> corpora;
    Corpus::List(input_path, source_lang, target_lang, corpora);

    Builder builder(builderOptions);
    builder.setListener(&listener);

    builder.Build(corpora, model_path);
}
