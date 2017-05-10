//
// Created by Davide Caroselli on 04/09/16.
//

#include <iostream>
#include <fstream>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <fastalign/Vocabulary.h>
#include <fastalign/BidirectionalModel.h>

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
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Train a SuffixArray Phrase Table from scratch");
    desc.add_options()
            ("help,h", "print this help message")
            ("model,m", po::value<string>()->required(), "output model path")
            ("output,o", po::value<string>()->required(), "output folder for the corpora alignment");

    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            std::cout << desc << std::endl;
            return false;
        }

        po::notify(vm);

        args->model_path = vm["model"].as<string>();
        args->output_path = vm["output"].as<string>();
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

    if (!fs::is_directory(args.model_path)) {
        cerr << "ERROR: model path is not a valid directory" << endl;
        return GENERIC_ERROR;
    }

    // Load models
    fs::path model_filename = fs::absolute(fs::path(args.model_path) / fs::path("model.dat"));
    fs::path vb_filename = fs::absolute(fs::path(args.model_path) / fs::path("model.voc"));

    if (!fs::is_regular(vb_filename))
        throw invalid_argument("File not found: " + vb_filename.string());
    if (!fs::is_regular(model_filename))
        throw invalid_argument("File not found: " + model_filename.string());

    BidirectionalModel *forward;
    BidirectionalModel *backward;

    const Vocabulary *vb = new Vocabulary(vb_filename.string());
    BidirectionalModel::Open(model_filename.string(), (Model **)&forward, (Model **)&backward);

    forward->ExportLexicalModel(args.output_path, vb);

    delete forward;
    delete backward;

    return SUCCESS;
}