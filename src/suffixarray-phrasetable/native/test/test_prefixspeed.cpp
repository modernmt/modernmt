#include <iostream>

#include <mmt/sentence.h>
#include <sapt/PhraseTable.h>
#include <suffixarray/UpdateBatch.h>
#include <suffixarray/SuffixArray.h>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <util/BilingualCorpus.h>
#include <test/util/NGramTable.h>
#include <util/chrono.h>

using namespace std;
using namespace mmt;
using namespace mmt::sapt;
using namespace mmt::sapt::test;

namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t SUCCESS = 0;

    struct args_t {
        string model_path;
        string input_path;
        string source_lang;
        string target_lang;

        domain_t domain;
        context_t context;
        uint8_t order = 16;
    };

    struct speed_perf_t {
        double seconds;
        size_t requests;

        speed_perf_t() : seconds(0.), requests(0) {};

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
    po::options_description desc("Test the SuffixArray querying the input domain");
    desc.add_options()
            ("help,h", "print this help message")
            ("model,m", po::value<string>()->required(), "input model path")
            ("source,s", po::value<string>()->required(), "source language")
            ("target,t", po::value<string>()->required(), "target language")
            ("input,i", po::value<string>()->required(), "input folder with input corpora")
            ("domain,d", po::value<domain_t>()->required(), "domain for data loading")
            ("context,c", po::value<string>(), "context map in the format <id>:<w>[,<id>:<w>]")
            ("order", po::value < unsigned
    int > (), "order (default = 16)");

    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            std::cout << desc << std::endl;
            return false;
        }

        po::notify(vm);

        args->model_path = vm["model"].as<string>();
        args->input_path = vm["input"].as<string>();
        args->source_lang = vm["source"].as<string>();
        args->target_lang = vm["target"].as<string>();
        args->domain = vm["domain"].as<domain_t>();

        if (vm.count("context")) {
            if (!ParseContextMap(vm["context"].as<string>(), args->context))
                throw po::error("invalid context map: " + vm["context"].as<string>());
        }

        if (vm.count("order"))
            args->order = vm["order"].as < unsigned
        int > ();
    } catch (po::error &e) {
        std::cerr << "ERROR: " << e.what() << std::endl << std::endl;
        std::cerr << desc << std::endl;
        return false;
    }

    return true;
}

NGramTable LoadTable(const args_t &args) {
    BilingualCorpus corpus(args.domain,
                           args.input_path + "/" + to_string(args.domain) + "." + args.source_lang,
                           args.input_path + "/" + to_string(args.domain) + "." + args.target_lang,
                           args.input_path + "/" + to_string(args.domain) + ".align"
    );

    cout << "Loading domain... " << flush;
    NGramTable nGramTable(args.order);
    nGramTable.Load(corpus);
    cout << "DONE" << endl;

    return nGramTable;
}

// ------ Testing

void RunTest(SuffixArray &index, const context_t *context,
             const unordered_map<vector<wid_t>, size_t, phrase_hash> &ngrams, vector<speed_perf_t> &speedData) {
    size_t queryCount = 0;

    for (auto entry = ngrams.begin(); entry != ngrams.end(); ++entry) {
        Collector *collector = index.NewCollector(context, true);

        for (size_t i = 0; i < entry->first.size(); ++i) {
            double begin = GetTime();
            vector<sample_t> samples;
            collector->Extend(entry->first[i], 1000, samples);
            speedData[i].seconds += GetElapsedTime(begin);
            speedData[i].requests++;

            queryCount++;

            if (queryCount % 10000 == 0)
                cout << "." << flush;
        }

        delete collector;
    }
}

// --------------

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    if (!fs::exists(args.input_path) || !fs::is_directory(args.input_path)) {
        cerr << "ERROR: input path is not a valid directory" << endl;
        return GENERIC_ERROR;
    }

    Options options;
    SuffixArray index(args.model_path, options.prefix_length, options.gc_timeout, options.gc_buffer_size);

    NGramTable nGramTable = LoadTable(args);

    unordered_map<vector<wid_t>, size_t, phrase_hash> ngrams = nGramTable.GetNGrams(args.order);
    cout << "Running test" << flush;
    vector<speed_perf_t> speedData(args.order);
    RunTest(index, &args.context, ngrams, speedData);
    cout << " DONE" << endl;

    cout << endl << "Results:" << endl;
    for (uint8_t i = 0; i < args.order; ++i) {
        speed_perf_t speed = speedData[i];

        cout << "  - " << (i + 1) << "-grams: " << speed.requests << " queries in " << speed.seconds
             << " seconds, speed is " << (int) round(((double) speed.requests) / speed.seconds) << " q/s" << endl;
    }

    return SUCCESS;
}