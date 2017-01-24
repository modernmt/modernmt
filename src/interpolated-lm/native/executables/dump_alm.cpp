//
// Created by Nicola Bertoldi on 16/01/17.
//

#include <cstddef>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <iostream>
#include <db/NGramStorage.h>
#include <sys/time.h>
#include <fstream>


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
        uint8_t order = 0x5;
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
            ("dump,d", po::value<string>(),
             "output file where dump the content of the database (default is /dev/stdout)");

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

    NGramStorage storage(args.model_path, args.order);
    cerr << "Model loaded." << endl;

    domain_t domain;
    dbkey_t key;
    counts_t val;

    ofstream output(args.dump_file.c_str());

    StorageIterator *iterator = storage.NewIterator();
    while (iterator->Next(&domain, &key, &val)) {
        output << "domain " << domain
               << " key " << key
               << " count " << val.count
               << " successors " << val.successors
               << endl;
    }
    delete iterator;

    cerr << "Dump ended" << endl;

    return SUCCESS;
}
