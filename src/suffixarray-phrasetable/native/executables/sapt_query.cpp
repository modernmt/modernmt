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
        bool quiet = false;
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;

bool ParseContextMap(const string &str, context_t &context) {
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

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Query the SuffixArray Phrase Table");
    desc.add_options()
            ("help,h", "print this help message")
            ("model,m", po::value<string>()->required(), "output model path")
            ("context,c", po::value<string>(), "context map in the format <id>:<w>[,<id>:<w>]")
            ("quiet,q", "prints only number of match");

    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            std::cout << desc << std::endl;
            return false;
        }

        if (vm.count("context")) {
            if (!ParseContextMap(vm["context"].as<string>(), args->context))
                throw po::error("invalid context map: " + vm["context"].as<string>());
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

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    Options options;
    SuffixArray index(args.model_path, options.prefix_length, options.max_option_length);

    string line;
    vector<wid_t> sentence;

    context_t *context = args.context.empty() ? NULL : &args.context;

    while (getline(cin, line)) {
        ParseSentenceLine(line, sentence);

        vector<sample_t> samples;
        index.GetRandomSamples(sentence, 1000, samples, context);

        if (args.quiet) {
            cout << "Found " << samples.size() << " samples" << endl;
        }
    }

    return SUCCESS;
}