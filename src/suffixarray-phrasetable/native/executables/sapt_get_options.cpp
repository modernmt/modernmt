#include <iostream>

#include <mmt/sentence.h>
#include <sapt/PhraseTable.h>
#include <suffixarray/UpdateBatch.h>
#include <suffixarray/SuffixArray.h>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t SUCCESS = 0;

    struct args_t {
        string model_path;
        bool quiet = false;
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Extract and core translation options from SuffixArray Phrase Table");
    desc.add_options()
            ("help,h", "print this help message")
            ("model,m", po::value<string>()->required(), "output model path")
            ("quiet,q", "prints only number of match");

    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            std::cout << desc << std::endl;
            return false;
        }

        po::notify(vm);

        args->model_path = vm["model"].as<string>();

        if (vm.count("quiet"))
            args->quiet = true;
    } catch (po::error &e) {
        std::cerr << "ERROR: " << e.what() << std::endl << std::endl;
        std::cerr << desc << std::endl;
        return false;
    }

    return true;
}

static inline void ParseSentenceLine(const string &line, vector<wid_t> &output) {
    output.clear();

    stringstream stream(line);
    wid_t word;

    while (stream >> word) {
        output.push_back(word);
    }

}

static inline void ParseAlignmentLine(const string &line, alignment_t &alignment) {
    alignment.clear();

    stringstream stream(line);
    std::string pointString;

    while (stream >> pointString) {
        alignmentPoint_t point;
        unsigned long pos = pointString.find('-');
        length_t sourcePos = (length_t) atoi(pointString.substr(0,pos).c_str());
        length_t targetPos = (length_t) atoi(pointString.substr(pos+1).c_str());
        point.first = sourcePos;
        point.second = targetPos;
        alignment.push_back(point);
    }

}

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    Options options;

    PhraseTable pt(args.model_path, options);
    std::cerr << "Model loaded" << std::endl;

    string line;

    std::vector<TranslationOption> outOptions;

    context_t *context_vec = new context_t;
    context_vec->push_back(cscore_t(1,1.0));
    std::cerr << "context_vec->size():" << context_vec->size() << std::endl;

    while (getline(cin, line)) {
        std::cerr << "Reading line:" << line << std::endl;

        vector<wid_t> sourcePhrase;
        ParseSentenceLine(line, sourcePhrase);

        pt.GetTargetPhraseCollection(sourcePhrase, outOptions, context_vec);

        std::cerr << "Found " << outOptions.size()  << " options" << std::endl;

        for (auto option = outOptions.begin(); option != outOptions.end(); ++ option){

            std::cerr << "targetPhrase:|";
            for (auto w = option->targetPhrase.begin(); w != option->targetPhrase.end(); ++w) { std::cerr << *w << " "; }
            std::cerr << "|";
            std::cerr << std::endl;
        }
        std::cerr << "Finished" << std::endl;

    }

    return SUCCESS;
}