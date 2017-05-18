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
        string input_path;
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Train a SuffixArray Phrase Table from scratch");
    desc.add_options()
            ("help,h", "print this help message")
            ("vocabulary,v", po::value<string>()->required(), "vocabulary model file")
            ("input,i", po::value<string>(), "input folder or file (if missing, STDIN will be used instead)");

    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            std::cout << desc << std::endl;
            return false;
        }

        po::notify(vm);

        args->vocabulary_path = vm["vocabulary"].as<string>();
        if (vm.count("input"))
            args->input_path = vm["input"].as<string>();
    } catch (po::error &e) {
        std::cerr << "ERROR: " << e.what() << std::endl << std::endl;
        std::cerr << desc << std::endl;
        return false;
    }

    return true;
}

template<typename S> inline void EncodeStream(Vocabulary &vocabulary, S &stream) {
    for (string line; std::getline(stream, line);) {
        vector<string> tokens;

        stringstream ss(line);
        for (string word; ss >> word;)
            tokens.push_back(word);

        sentence_t sentence;
        vocabulary.Lookup(tokens, sentence, false);

        for (size_t i = 0; i < sentence.size(); ++i) {
            if (i > 0)
                cout << ' ';
            cout << sentence[i];
        }
        cout << '\n';
    }
}

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    Vocabulary vocabulary(args.vocabulary_path, true);

    if (args.input_path.empty()) {
        EncodeStream(vocabulary, std::cin);
    } else {
        fs::path input_path(args.input_path);

        if (fs::is_regular_file(input_path)) {
            ifstream in(args.input_path);
            EncodeStream(vocabulary, in);
        } else if (fs::is_directory(input_path)) {
            vector<string> files;

            fs::directory_iterator end;
            for (fs::directory_iterator it(input_path); it != end; ++it) {
                if (fs::is_regular_file(it->path()))
                    files.push_back(it->path().string());
            }

            std::sort(files.begin(), files.end());

            for (auto file = files.begin(); file != files.end(); ++file) {
                ifstream in(*file);
                EncodeStream(vocabulary, in);
            }
        } else {
            throw invalid_argument("Invalid input path: " + args.input_path);
        }
    }

    return SUCCESS;
}