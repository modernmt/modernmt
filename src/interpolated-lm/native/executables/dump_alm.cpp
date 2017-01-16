//
// Created by Nicola Bertoldi on 16/01/17.
//

#include <cstddef>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <iostream>
#include <fstream>
#include <db/NGramStorage.h>
#include <sys/time.h>


using namespace std;
using namespace mmt;
using namespace mmt::ilm;

namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t SUCCESS = 0;

    struct args_t {
        string model_path;
        string dump_file = "/dev/stdout";
        uint8_t order = 5;
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;

#define PrintUsage(name) {cerr << "USAGE: " << name << " [-h] [-o ARG] [-d ARG] -m MODEL_PATH" << endl << endl;}

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("dump the content of the adaptive language model");
    desc.add_options()
            ("help,h", "print this help message")
            ("model,m", po::value<string>()->required(), "output model path")
            ("dump,d", po::value<string>(), "output file where dump the content of the database (default is /dev/stdout)")
            ("order,o", po::value<uint8_t>(), "the language model order (default is 5)");

    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            std::cout << desc << std::endl;
            return false;
        }

        po::notify(vm);

        args->model_path = vm["model"].as<string>();
        if (vm.count("dump"))
            args->dump_file = vm["dump"].as<string>();

        if (vm.count("order"))
            args->order = vm["order"].as<uint8_t>();
    } catch (po::error &e) {
        std::cerr << "ERROR: " << e.what() << std::endl << std::endl;
        std::cerr << desc << std::endl;
        return false;
    }

    return true;
}

double GetTime() {
    struct timeval time;

    if (gettimeofday(&time, NULL)) {
        //  Handle error
        return 0;
    }

    return (double) time.tv_sec + ((double) time.tv_usec / 1000000.);
}

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    if (!fs::exists(args.model_path) || !fs::is_directory(args.model_path)) {
        cerr << "ERROR: model path is not a valid directory" << endl;
        return GENERIC_ERROR;
    }

    NGramStorage storage(args.model_path, args.order, true);
    cerr << "Model loaded." << endl;

    float count, successors;
    domain_t domain;
    dbkey_t key;

    ofstream output(args.dump_file.c_str());

    //scan the lm to get all keys and values
    storage.ScanInit();
    while (storage.ScanNext(domain, key, count, successors)){
        output << "domain:" << domain;
        output << " key:" << key;
        output << " count:" << count;
        output << " successors:" << successors;
        output << endl;
    }
    storage.ScanTerminate();

    return SUCCESS;
}
