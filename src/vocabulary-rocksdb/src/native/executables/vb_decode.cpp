//
// Created by Davide  Caroselli on 15/05/17.
//

#include <iostream>
#include <fstream>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <Vocabulary.h>

using namespace std;
using namespace mmt;

namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t SUCCESS = 0;

    struct args_t {
        string vocabulary_path;
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Train a SuffixArray Phrase Table from scratch");
    desc.add_options()
            ("help,h", "print this help message")
            ("vocabulary,v", po::value<string>()->required(), "vocabulary model file");

    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            std::cout << desc << std::endl;
            return false;
        }

        po::notify(vm);

        args->vocabulary_path = vm["vocabulary"].as<string>();
    } catch (po::error &e) {
        std::cerr << "ERROR: " << e.what() << std::endl << std::endl;
        std::cerr << desc << std::endl;
        return false;
    }

    return true;
}

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    Vocabulary vocabulary(args.vocabulary_path, true);

    for (string line; std::getline(std::cin, line);) {
        sentence_t sentence;

        stringstream ss(line);
        for (wid_t id; ss >> id;)
            sentence.push_back(id);

        vector<string> tokens;
        vocabulary.ReverseLookup(sentence, tokens);

        for (size_t i = 0; i < tokens.size(); ++i) {
            if (i > 0)
                cout << ' ';
            cout << tokens[i];
        }
        cout << '\n';
    }

    return SUCCESS;
}