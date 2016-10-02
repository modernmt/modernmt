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
    po::options_description desc("Query the SuffixArray Phrase Table");
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

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    Options options;
    SuffixArray index(args.model_path, options.prefix_length, options.max_option_length);

    std::cerr << "Model loaded" << std::endl;

    vector<sample_t> samples;
    index.GetRandomSamples(vector<wid_t>(), 1, samples, NULL);
    std::cerr << "Sample loaded with no context" << std::endl;

    string line;
    vector<wid_t> sourcePhrase;

    //context_t *context_vec = NULL;
    context_t *context_vec = new context_t;
    context_vec->push_back(cscore_t(1,1.0));
    std::cerr << "context_vec->size():" << context_vec->size() << std::endl;

    while (getline(cin, line)) {
        std::cerr << "Reading line:" << line << std::endl;
        ParseSentenceLine(line, sourcePhrase);

        cout << "sourcePhrase.size():" << sourcePhrase.size()  << endl;
        std::cerr << "SourcePhrase:|";
        for (auto w = sourcePhrase.begin(); w != sourcePhrase.end(); ++w) { std::cerr << *w << " "; }
        std::cerr << "|" << std::endl;

        vector<sample_t> samples;
        /*index.GetRandomSamples(1, sourcePhrase, 100, samples);
        cout << "Found " << samples.size() << " samples" << endl;
        index.GetRandomSamples(1, sourcePhrase, 100, samples);
        cout << "Found " << samples.size() << " samples" << endl;
        */
        index.GetRandomSamples(sourcePhrase, 100, samples, context_vec);

        if (args.quiet) {
            cout << "Found " << samples.size() << " samples" << endl;
        } else{
            cout << "Found " << samples.size() << " samples" << endl;
            for (auto sample = samples.begin(); sample != samples.end(); ++sample) {
                std::cerr << "Source:|";
                for (auto w = sample->source.begin(); w != sample->source.end(); ++w) { std::cerr << *w << " "; }
                std::cerr << "| Target:|";
                for (auto w = sample->target.begin(); w != sample->target.end(); ++w) { std::cerr << *w << " "; }
                std::cerr << "| Offset:|";
                for (auto o = sample->offsets.begin(); o != sample->offsets.end(); ++o) { std::cerr << *o << " "; }
                std::cerr << "| Alignemnt:|";
                for (auto a = sample->alignment.begin(); a != sample->alignment.end(); ++a) { std::cerr << a->first << "-" << a->second << " "; }
                std::cerr << "|";
                std::cerr << " Domain:|" << sample->domain << "|" << std::endl;

            }
        }
    }

    return SUCCESS;
}