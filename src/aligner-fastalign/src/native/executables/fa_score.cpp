//
// Created by Davide Caroselli on 04/09/16.
//

#include <iostream>
#include <random>
#include <fstream>
#include <fastalign/Corpus.h>
#include <fastalign/FastAligner.h>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <thread>
#include <cmath>

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

class Sequence {
public:
    Sequence() : sum(0), sum2(0), count(0) {}

    void Add(score_t score) {
        if (!isnan(score)) {
            sum += score;
            sum2 += score * score;
            count++;
        }
    }

    double GetAverage() {
        return sum / count;
    }

    double GetStandardDeviation() {
        return sqrt((sum2 / count) - (GetAverage() * GetAverage()));
    }

private:
    double sum;
    double sum2;
    double count;
};


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

void PrintScores(vector<alignment_t> &alignments, ofstream &out) {
    for (auto a = alignments.begin(); a != alignments.end(); ++a)
        out << a->score << endl;
}

void CollectScores(vector<alignment_t> &alignments, Sequence &seq) {
    for (auto a = alignments.begin(); a != alignments.end(); ++a)
        seq.Add(a->score);
}

void ShiftBatch(vector<pair<wordvec_t, wordvec_t>> &batch) {
    wordvec_t first = batch[0].second;
    for (size_t i = 1; i < batch.size(); ++i)
        batch[i - 1].second = batch[i].second;
    batch[batch.size() - 1].second = first;
}

void ScoreCorpus(FastAligner &aligner, Sequence &goodScores, Sequence &badScores,
                 const Corpus &corpus, size_t buffer_size, const string &outputPath) {
    CorpusReader reader(corpus, aligner.vocabulary);

    vector<pair<wordvec_t, wordvec_t>> batch;
    vector<alignment_t> alignments;

    string scorePath = (fs::path(outputPath) / (corpus.GetName() + ".score")).string();
    ofstream scoreStream;
    scoreStream.open(scorePath.c_str(), ios_base::out);

    while (reader.Read(batch, buffer_size)) {
        aligner.GetAlignments(batch, alignments, GrowDiagonalFinalAnd);

        PrintScores(alignments, scoreStream);
        CollectScores(alignments, goodScores);
        alignments.clear();

        if (batch.size() > 1) {
            ShiftBatch(batch);

            aligner.GetAlignments(batch, alignments, GrowDiagonalFinalAnd);
            CollectScores(alignments, badScores);
            alignments.clear();
        }

        batch.clear();
    }
}

int main(int argc, const char *argv[]) {
    int threads = 1;

#ifdef _OPENMP
    threads = thread::hardware_concurrency();

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
    Corpus::List(args.input_path, args.source_lang, args.target_lang, corpora);

    if (corpora.empty())
        exit(0);

    FastAligner aligner(args.model_path, threads);
    Sequence goodScores;
    Sequence badScores;

    // perform scoring of all corpora sequentially; multi-threading is used for each corpus
    for (auto corpus = corpora.begin(); corpus < corpora.end(); ++corpus) {
        ScoreCorpus(aligner, goodScores, badScores, *corpus, args.buffer_size, args.output_path);
    }

    cout << "good_avg=" << goodScores.GetAverage() << "\n";
    cout << "good_std_dev=" << goodScores.GetStandardDeviation() << "\n";
    cout << "bad_avg=" << badScores.GetAverage() << "\n";
    cout << "bad_std_dev=" << badScores.GetStandardDeviation() << "\n";

    return SUCCESS;
}