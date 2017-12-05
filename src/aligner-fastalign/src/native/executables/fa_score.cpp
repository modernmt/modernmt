//
// Created by Davide Caroselli on 04/09/16.
//

#include <iostream>
#include <fstream>
#include <fastalign/Corpus.h>
#include <fastalign/FastAligner.h>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <thread>

#ifdef _OPENMP
#include <thread>
#include <omp.h>
#endif

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;


namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t SUCCESS = 0;

    struct args_t {
        string output_path;
        string model_path;
        string input_path;
        string source_lang;
        string target_lang;

        size_t buffer_size = 100000;
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;



bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Train a SuffixArray Phrase Table from scratch");
    desc.add_options()
            ("help,h", "print this help message")
            ("model,m", po::value<string>()->required(), "output model path")
            ("output,o", po::value<string>()->required(), "output folder for the corpora alignment")
            ("source,s", po::value<string>()->required(), "source language")
            ("target,t", po::value<string>()->required(), "target language")
            ("input,i", po::value<string>()->required(), "input folder with input corpora")
            ("buffer,b", po::value<size_t>(), "size of the buffer");

    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            std::cout << desc << std::endl;
            return false;
        }

        po::notify(vm);

        args->model_path = vm["model"].as<string>();
        args->input_path = vm["input"].as<string>();
        args->output_path = vm["output"].as<string>();
        args->source_lang = vm["source"].as<string>();
        args->target_lang = vm["target"].as<string>();

        if (vm.count("buffer"))
            args->buffer_size = vm["buffer"].as<size_t>();
    } catch (po::error &e) {
        std::cerr << "ERROR: " << e.what() << std::endl << std::endl;
        std::cerr << desc << std::endl;
        return false;
    }

    return true;
}

void printScore(vector<double> &scores, ofstream &out) {
    for (auto a = scores.begin(); a != scores.end(); ++a) {
        out << *a << endl;
    }
}

void ScoreCorpus(const Corpus &corpus, size_t buffer_size, FastAligner &aligner) {
    cerr << "void ScoreCorpus START" << endl;
    CorpusReader reader(corpus, aligner.vocabulary);
    cerr << "void ScoreCorpus reader created" << endl;

    vector<pair<wordvec_t, wordvec_t>> batch;
    vector<double> scores;

    ofstream scoreStream(corpus.GetOutputScorePath().c_str());
    cerr << "void ScoreCorpus buffer_size" << buffer_size << endl;
    while (reader.Read(batch, buffer_size)) {
        cerr << "void ScoreCorpus batch.size()" << batch.size() << endl;
        aligner.GetScores(batch, scores);

        printScore(scores, scoreStream);

        scores.clear();
        batch.clear();
    }
}

int main(int argc, const char *argv[]) {
    int threads = 1;

#ifdef _OPENMP
    threads = std::min((thread::hardware_concurrency() * 2) / 3, 8U);

    omp_set_dynamic(0);
    omp_set_num_threads(threads);
#endif

    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    if (!fs::exists(args.input_path) || !fs::is_directory(args.input_path)) {
        cerr << "ERROR: input path is not a valid directory" << endl;
        return GENERIC_ERROR;
    }

    if (!fs::is_directory(args.model_path)) {
        cerr << "ERROR: model path is not a valid directory" << endl;
        return GENERIC_ERROR;
    }

    if (!fs::is_directory(args.output_path))
        fs::create_directories(args.output_path);

    vector<Corpus> corpora;
    Corpus::List(args.input_path, args.output_path, args.source_lang, args.target_lang, corpora);

    if (corpora.empty())
        exit(0);

    FastAligner aligner(args.model_path, threads);

    //perform alignment of all corpora sequentially; multithreading is used for each corpus
    for (size_t i = 0; i < corpora.size(); ++i) {
        Corpus &corpus = corpora[i];
        cerr << "HERE 1" << endl;
        ScoreCorpus(corpus, args.buffer_size, aligner);
        cerr << "HERE 2" << endl;
    }

    return SUCCESS;
}