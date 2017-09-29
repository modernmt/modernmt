//
// Created by Davide  Caroselli on 07/09/16.
//

#include <cstddef>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <iostream>
#include <db/NGramStorage.h>
#include <sys/time.h>
#include <corpus/CorpusReader.h>
#ifdef _OPENMP
#include <thread>
#include <omp.h>
#endif

using namespace std;
using namespace mmt;
using namespace mmt::ilm;

namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t SUCCESS = 0;

    struct args_t {
        string model_path;
        string input_path;
        string vocabulary_path;
        uint8_t order = 0x5;

        size_t buffer_size = 100000;
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Train an adaptive language model from scratch");
    desc.add_options()
            ("help,h", "print this help message")
            ("model,m", po::value<string>()->required(), "output model path")
            ("input,i", po::value<string>()->required(), "input folder with input corpora")
            ("vocabulary,v", po::value<string>()->required(), "vocabulary file")
            ("order,o", po::value<size_t>(), "the language model order (default is 5)")
            ("buffer,b", po::value<size_t>(), "size of the buffer expressed in number of n-grams");

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
        args->vocabulary_path = vm["vocabulary"].as<string>();

        if (vm.count("buffer"))
            args->buffer_size = vm["buffer"].as<size_t>();

        if (vm.count("order"))
            args->order = (uint8_t) vm["order"].as<size_t>();

    } catch (po::error &e) {
        std::cerr << "ERROR: " << e.what() << std::endl << std::endl;
        std::cerr << desc << std::endl;
        return false;
    }

    return true;
}

void ListCorpora(const string &root, vector<string> &outCorpora) {
    fs::recursive_directory_iterator endit;

    for (fs::recursive_directory_iterator it(root); it != endit; ++it) {
        if (fs::is_regular_file(*it))
            outCorpora.push_back(fs::absolute(it->path()).string());
    }
}

double GetTime() {
    struct timeval time;

    if (gettimeofday(&time, NULL)) {
        //  Handle error
        return 0;
    }

    return (double) time.tv_sec + ((double) time.tv_usec / 1000000.);
}

void LoadCorpus(Vocabulary &vb, const string &corpus, NGramStorage &storage, uint8_t order, size_t buffer_size) {
    memory_t memory = (memory_t) stoi(fs::path(corpus).stem().string());

    CorpusReader reader(vb, corpus);
    NGramBatch batch(order, buffer_size);
    size_t batches=1;
    vector<wid_t> sentence;
    while(reader.Read(sentence)) {
        if (!batch.Add(memory, sentence)) {
            storage.PutBatch(batch);

            ++batches;
            batch.Clear();
            batch.Add(memory, sentence);
        }
    }

    if (!batch.IsEmpty()) {
        storage.PutBatch(batch);
        batch.Clear();
    }
    cerr << "loading memory:" << memory << " requires " << batches << " batches" << endl;
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

    Vocabulary vb(args.vocabulary_path, true);

    vector<string> corpora;
    ListCorpora(args.input_path, corpora);

    NGramStorage storage(args.model_path, args.order, true);

#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < corpora.size(); ++i) {
        string &corpus = corpora[i];
        double begin = GetTime();
        LoadCorpus(vb, corpus, storage, args.order, args.buffer_size);
        double elapsed = GetTime() - begin;
        cout << "Corpus " << corpus << " DONE in " << elapsed << "s" << endl;
    }

    storage.ForceCompaction();
    return SUCCESS;
}