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
            ("model,m", po::value<string>()->required(), "input model path")
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

static void PrintSample(const sample_t &sample) {
    cout << "(" << sample.domain << ")";

    for (auto word = sample.source.begin(); word != sample.source.end(); ++word)
        cout << " " << *word;
    cout << " |||";
    for (auto word = sample.target.begin(); word != sample.target.end(); ++word)
        cout << " " << *word;
    cout << " |||";
    for (auto a = sample.alignment.begin(); a != sample.alignment.end(); ++a)
        cout << " " << a->first << "-" << a->second;
    cout << endl;
}

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    Options options;
    SuffixArray index(args.model_path, options.prefix_length);

    std::cerr << "Model loaded" << std::endl;

    string line;
    vector<wid_t> sourcePhrase;

    context_t *context = args.context.empty() ? NULL : &args.context;
    if (context){
        std::cerr << "context->size():" << context->size() << std::endl;
    } else{
        std::cerr << "context not provided" << std::endl;
    }

    while (getline(cin, line)) {
        std::cerr << "Reading line:" << line << std::endl;
        ParseSentenceLine(line, sourcePhrase);

        cout << "sourcePhrase.size():" << sourcePhrase.size()  << endl;
        std::cerr << "SourcePhrase:|";
        for (auto w = sourcePhrase.begin(); w != sourcePhrase.end(); ++w) { std::cerr << *w << " "; }
        std::cerr << "|" << std::endl;

        vector<sample_t> samples;
        /*
        index.GetRandomSamples(1, sourcePhrase, 100, samples);
        cout << "Found " << samples.size() << " samples" << endl;
        index.GetRandomSamples(1, sourcePhrase, 100, samples);
        cout << "Found " << samples.size() << " samples" << endl;
        */
        index.GetRandomSamples(sourcePhrase, 100, samples, context);

        if (!args.quiet) {
            for (size_t i = 0; i < samples.size(); i++)
                PrintSample(samples[i]);
        }

        cout << "Found " << samples.size() << " samples" << endl;
    }

    return SUCCESS;
}
