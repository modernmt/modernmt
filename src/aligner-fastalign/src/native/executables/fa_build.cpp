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
#include <sys/time.h>
#include <fastalign/Builder.h>
#include <fastalign/FastAligner.h>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t SUCCESS = 0;

    struct args_t {
        string source_lang;
        string target_lang;
        string input_path;
        string model_path;

        Options options = Options();
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;


bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Train a new FastAlign model from a collection of parallel files");
    desc.add_options()
            ("help,h", "print this help message")
            ("source,s", po::value<string>()->required(), "source language")
            ("target,t", po::value<string>()->required(), "target language")
            ("input,i", po::value<string>()->required(), "input folder containing the parallel files collection")
            ("model,m", po::value<string>()->required(), "the output path")
            ("threads,T", po::value<unsigned int>(), "number of threads (default is number of CPU)")
            ("iterations,I", po::value<unsigned int>(), "number of iterations in EM training (default is 5)")
            ("prune,p", po::value<double>(), "final model pruning threshold (default is 1.e-20)")
            ("vocabulary-thr,v", po::value<double>(), "keeps only the most relevant terms in vocabulary "
                                                      "(default is 0.9999 - only the terms that cover "
                                                      "the 99.99% of the input corpora)")
            ("max-length,l", po::value<size_t>(), "max sentence length (default is 80)")
            ("case-insensitive", "create a case insensitive model (default is case sensitive)")
            ("no-favor-diagonal", "don't enforce diagonal form of alignment (default is use diagonal)");

    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            std::cout << desc << std::endl;
            return false;
        }

        po::notify(vm);
        args->source_lang = vm["source"].as<string>();
        args->target_lang = vm["target"].as<string>();
        args->input_path = vm["input"].as<string>();
        args->model_path = vm["model"].as<string>();

        if (vm.count("threads"))
            args->options.threads = vm["threads"].as<unsigned int>();
        if (vm.count("iterations"))
            args->options.iterations = vm["iterations"].as<unsigned int>();
        if (vm.count("prune"))
            args->options.pruning_threshold = vm["prune"].as<double>();
        if (vm.count("vocabulary-thr"))
            args->options.vocabulary_threshold = vm["vocabulary-thr"].as<double>();
        if (vm.count("max-length"))
            args->options.max_line_length = vm["max-length"].as<size_t>();

        if (vm.count("case-insensitive"))
            args->options.case_sensitive = false;
        if (vm.count("no-favor-diagonal"))
            args->options.favor_diagonal = false;
    } catch (po::error &e) {
        std::cerr << "ERROR: " << e.what() << std::endl << std::endl;
        std::cerr << desc << std::endl;
        return false;
    }

    return true;
}

class ProcessListener : public Builder::Listener {
public:
    void BuildStart(const std::string &opts) override {
        cerr << "Build started with options: " << opts << endl;
    }

    void VocabularyBuildBegin() override {
        cerr << "Creating vocabulary... ";
        stepBegin = GetTime();
    }

    void VocabularyBuildEnd() override {
        cerr << "DONE in " << (GetTime() - stepBegin) << "s" << endl;
    }

    void Begin(bool forward) override {
        processBegin = GetTime();
        cerr << "== " << (forward ? "Forward" : "Backward") << " model training ==" << endl;
    }

    void IterationBegin(bool forward, int iteration) override {
        cerr << "Iteration " << iteration << ":" << endl;
    }

    void Begin(bool forward, const BuilderStep step, int iteration) override {
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

    void End(bool forward, const BuilderStep step, int iteration) override {
        cerr << "DONE in " << (GetTime() - stepBegin) << "s" << endl;
    }

    void IterationEnd(bool forward, int iteration) override {
        // Nothing to do
    }

    void End(bool forward) override {
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
    double stepBegin = 0;
    double processBegin = 0;

    double GetTime() {
        struct timeval time{};

        if (gettimeofday(&time, nullptr)) {
            //  Handle error
            return 0;
        }

        return (double) time.tv_sec + ((double) time.tv_usec / 1000000.);
    }
};

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    ProcessListener listener;
    vector<Corpus> corpora;
    Corpus::List(args.input_path, args.source_lang, args.target_lang, corpora);

    Builder builder(args.options);
    builder.setListener(&listener);

    builder.Build(corpora, args.model_path);

    return SUCCESS;
}
