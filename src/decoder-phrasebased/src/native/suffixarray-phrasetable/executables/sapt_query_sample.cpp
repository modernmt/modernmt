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
        size_t sample_limit = 1000;
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
            cout << desc << endl;
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
        cerr << "ERROR: " << e.what() << endl << endl;
        cerr << desc << endl;
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

    context_t *context = args.context.empty() ? NULL : &args.context;

    Options options;
    options.samples = args.sample_limit;

    SuffixArray sa(args.model_path, options.prefix_length, options.gc_timeout, options.gc_buffer_size);

    if (!args.quiet) {
        cout << "Model loaded" << endl;
        if (context) {
            cout << "context->size():" << context->size() << endl;
        } else {
            cout << "context not provided" << endl;
        }
    }

    string line;
    vector<wid_t> sourcePhrase;

    while (getline(cin, line)) {
        ParseSentenceLine(line, sourcePhrase);

        if (!args.quiet) {
            cout << "SourcePhrase:";
            for (auto w = sourcePhrase.begin(); w != sourcePhrase.end(); ++w) { cout << *w << " "; }
            cout << endl;
        }

        vector<sample_t> samples;
        sa.GetRandomSamples(sourcePhrase, args.sample_limit, samples, context);

        size_t NumberOfsamples = samples.size();

        if (!args.quiet) {
            for (size_t pos = 0; pos < NumberOfsamples; ++pos) {
                const sample_t &sample = samples[pos];

                for (auto w = sample.source.begin(); w != sample.source.end(); ++w) {
                    cout << *w << " ";
                }
                cout << "||| ";
                for (auto w = sample.target.begin(); w != sample.target.end(); ++w) {
                    cout << *w << " ";
                }
                cout << "||| ";
                for (auto a = sample.alignment.begin(); a != sample.alignment.end(); ++a) {
                    cout << a->first << "-" << a->second << " ";
                }
                cout << endl;
            }
        }
        cout << "Found " << NumberOfsamples << " samples" << endl;
    }

    return SUCCESS;
}