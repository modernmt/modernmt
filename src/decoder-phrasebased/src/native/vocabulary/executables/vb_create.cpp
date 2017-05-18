//
// Created by Davide  Caroselli on 15/05/17.
//

#include <iostream>
#include <fstream>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <Vocabulary.h>

#ifdef _OPENMP
#include <thread>
#include <omp.h>
#endif

using namespace std;
using namespace mmt;

namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t SUCCESS = 0;

    struct args_t {
        string output_path;
        string input_path;
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Train a SuffixArray Phrase Table from scratch");
    desc.add_options()
            ("help,h", "print this help message")
            ("output,o", po::value<string>()->required(), "vocabulary model output file")
            ("input,i", po::value<string>(), "input folder or file (if missing, STDIN will be used instead)");

    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            std::cout << desc << std::endl;
            return false;
        }

        po::notify(vm);

        args->output_path = vm["output"].as<string>();

        if (vm.count("input"))
            args->input_path = vm["input"].as<string>();
    } catch (po::error &e) {
        std::cerr << "ERROR: " << e.what() << std::endl << std::endl;
        std::cerr << desc << std::endl;
        return false;
    }

    return true;
}

template<typename S> inline void ReadStream(S &stream, unordered_set<string> &output) {
    for (string term; stream >> term;)
        output.insert(term);
}

void LoadTermsFromFiles(const vector<string> &files, unordered_set<string> &output) {
#ifdef _OPENMP
    size_t threads = thread::hardware_concurrency();

    omp_set_dynamic(0);
    omp_set_num_threads(threads);
#else
    size_t threads = 1;
#endif

    vector<unordered_set<string>> sets(threads);

#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < files.size(); ++i) {
#ifdef _OPENMP
        unordered_set<string> &set = sets[omp_get_thread_num()];
#else
        unordered_set<string> &set = sets[0];
#endif

        ifstream in(files[i]);
        ReadStream(in, set);
    }

    for (auto set = sets.begin(); set != sets.end(); ++set) {
        output.insert(set->begin(), set->end());
    }
}

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    unordered_set<string> terms;

    fs::path output_path(args.output_path);
    if (fs::is_regular_file(output_path))
        fs::remove(output_path);

    if (args.input_path.empty()) {
        ReadStream(std::cin, terms);
    } else {
        fs::path input_path(args.input_path);

        if (fs::is_regular_file(input_path)) {
            ifstream in(args.input_path);
            ReadStream(in, terms);
        } else if (fs::is_directory(input_path)) {
            vector<string> files;

            fs::directory_iterator end;
            for (fs::directory_iterator it(input_path); it != end; ++it) {
                if (fs::is_regular_file(it->path()))
                    files.push_back(it->path().string());
            }

            LoadTermsFromFiles(files, terms);
        } else {
            throw invalid_argument("Invalid input path: " + args.input_path);
        }
    }

    Vocabulary(args.output_path, false, &terms);

    return SUCCESS;
}