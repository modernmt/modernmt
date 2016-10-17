#include <iostream>

#include <mmt/sentence.h>
#include <sapt/PhraseTable.h>
#include <suffixarray/UpdateBatch.h>
#include <suffixarray/SuffixArray.h>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <thread>
#include <util/BilingualCorpus.h>
#include <util/chrono.h>
#ifdef _OPENMP
#include <thread>
#include <omp.h>
#endif

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t SUCCESS = 0;

    struct args_t {
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

void LoadCorpus(const BilingualCorpus &corpus, SuffixArray &index, size_t buffer_size) {
    domain_t domain = corpus.GetDomain();

    CorpusReader reader(corpus);
    UpdateBatch batch(buffer_size, vector<seqid_t>());

    vector<wid_t> source;
    vector<wid_t> target;
    alignment_t alignment;

    while(reader.Read(source, target, alignment)) {
        if (!batch.Add(domain, source, target, alignment)) {
            index.PutBatch(batch);

            batch.Clear();
            batch.Add(domain, source, target, alignment);
        }
    }

    if (batch.GetSize() > 0) {
        index.PutBatch(batch);
        batch.Clear();
    }
}

int main(int argc, const char *argv[]) {
#ifdef _OPENMP
    int threads = std::min((thread::hardware_concurrency() * 2) / 3, 8U);

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

    if (!fs::is_directory(args.model_path))
        fs::create_directories(args.model_path);

    Options options;
    SuffixArray index(args.model_path, options.prefix_length, true);

    vector<BilingualCorpus> corpora;
    BilingualCorpus::List(args.input_path, args.source_lang, args.target_lang, corpora);

#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < corpora.size(); ++i) {
        BilingualCorpus &corpus = corpora[i];

        double begin = GetTime();
        LoadCorpus(corpus, index, args.buffer_size);
        double elapsed = GetElapsedTime(begin);
        cout << "Corpus " << corpus.GetDomain() << " DONE in " << elapsed << "s" << endl;
    }

    index.ForceCompaction();

    return SUCCESS;
}