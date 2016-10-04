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
        context_t context;
        size_t sample_limit = 100;
        bool quiet = false;
    };
} // namespace

bool ParseContext(const string &str, context_t &context) {
    istringstream iss(str);
    string element;

    while (getline(iss, element, ',')) {
        istringstream ess(element);

        string tok;
        getline(ess, tok, ':');
        domain_t id = (domain_t) stoi(tok);
        getline(ess, tok, ':');
        float w = stof(tok);

        context.push_back(cscore_t(id, w));
    }

    return true;
}

namespace po = boost::program_options;
namespace fs = boost::filesystem;

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Extract and core translation options from SuffixArray Phrase Table");
    desc.add_options()
            ("help,h", "print this help message")
            ("model,m", po::value<string>()->required(), "output model path")
            ("context,c", po::value<string>(), "context map in the format <id>:<w>[,<id>:<w>]")
            ("sample,s", po::value<size_t>(), "number of samples (default is 100)")
            ("quiet,q", "prints only number of match");


    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            std::cout << desc << std::endl;
            return false;
        }

        if (vm.count("context")) {
            if (!ParseContext(vm["context"].as<string>(), args->context))
                throw po::error("invalid context map: " + vm["context"].as<string>());
        }

        po::notify(vm);

        args->model_path = vm["model"].as<string>();

        if (vm.count("sample"))
            args->sample_limit = vm["sample"].as<size_t>();

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

int main(int argc, const char *argv[]) {

    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    Options options;

    PhraseTable pt(args.model_path, options);
    if (!args.quiet) pt.setDebug(true);

    std::cout << "Model loaded" << std::endl;

    string line;

    std::vector<TranslationOption> outOptions;


    context_t *context = args.context.empty() ? NULL : &args.context;
    if (context){
        std::cout << "context->size():" << context->size() << std::endl;
    } else{
        std::cout << "context not provided" << std::endl;
    }
    size_t sample_limit = args.sample_limit;

    while (getline(cin, line)) {

        vector<wid_t> sourcePhrase;
        ParseSentenceLine(line, sourcePhrase);

        std::cout << "SourcePhrase:|";
        for (auto w = sourcePhrase.begin(); w != sourcePhrase.end(); ++w) { std::cout << *w << " "; }
        std::cout << "|" << std::endl;

        pt.GetTargetPhraseCollection(sourcePhrase, sample_limit, outOptions, context);

        if (!args.quiet) {
            for (auto option = outOptions.begin(); option != outOptions.end(); ++option) {
                std::cout << *option << endl;
            }
        }
    }

    return SUCCESS;
}