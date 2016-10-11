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
        size_t sample_limit;
        bool quiet = false;
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;

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

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Query the SuffixArray Phrase Table");
    desc.add_options()
            ("help,h", "print this help message")
            ("model,m", po::value<string>()->required(), "input model path")
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

size_t CountSamples(const vector<sample_t> &samples) {
    size_t count = 0;
    for (auto sample = samples.begin(); sample != samples.end(); ++sample) {
        count += sample->offsets.size();
    }
    return count;
}

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    context_t *context = args.context.empty() ? NULL : &args.context;

    size_t sample_limit = args.sample_limit;

    Options options;
    SuffixArray index(args.model_path, options.prefix_length);

    if (!args.quiet) {
        std::cerr << "Model loaded" << std::endl;
        if (context) {
            std::cerr << "context->size():" << context->size() << std::endl;
        } else {
            std::cerr << "context not provided" << std::endl;
        }
    }


    string line;
    vector<wid_t> sourcePhrase;

    while (getline(cin, line)) {
        ParseSentenceLine(line, sourcePhrase);

        if (!args.quiet) {
            std::cerr << "SourcePhrase: ";
            for (auto w = sourcePhrase.begin(); w != sourcePhrase.end(); ++w) { std::cerr << " " << *w; }
            std::cerr << std::endl;
        }

        // -------------------------------
        vector<sample_t> samples2;
        Collector *collector = index.NewCollector(context);
        for (auto word = sourcePhrase.begin(); word != sourcePhrase.end(); ++word) {
            samples2.clear();
            collector->Extend(*word, sample_limit, samples2);
        }
        delete collector;

//        if (!args.quiet) {
//            for (auto sample = samples2.begin(); sample != samples2.end(); ++sample)
//                std::cout << sample->ToString() << endl;
//        }

        cout << "Iterative found " << CountSamples(samples2) << " samples" << endl;
        // -------------------------------

        vector<sample_t> samples;
        index.GetRandomSamples(sourcePhrase, sample_limit, samples, context);

        if (!args.quiet) {
            for (auto sample = samples.begin(); sample != samples.end(); ++sample)
                std::cout << sample->ToString() << endl;
        }

        cout << "Found " << CountSamples(samples) << " samples" << endl;
    }

    return SUCCESS;
}
