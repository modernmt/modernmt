#include <iostream>

#include <mmt/sentence.h>
#include <sapt/PhraseTable.h>
#include <suffixarray/UpdateBatch.h>
#include <suffixarray/SuffixArray.h>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <util/BilingualCorpus.h>
#include <test/util/NGramTable.h>

using namespace std;
using namespace mmt;
using namespace mmt::sapt;
using namespace mmt::sapt::test;

namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t TEST_FAILED = 3;
    const size_t SUCCESS = 0;

    struct args_t {
        string model_path;
        string input_path;
        string source_lang;
        string target_lang;

        uint8_t order = 6;
        bool is_source = true;
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("Test the SuffixArray querying the input domain");
    desc.add_options()
            ("help,h", "print this help message")
            ("model,m", po::value<string>()->required(), "input model path")
            ("source,s", po::value<string>()->required(), "source language")
            ("target,t", po::value<string>()->required(), "target language")
            ("input,i", po::value<string>()->required(), "input folder with input corpora")
            ("use-target", "search in target side")
            ("order", po::value<uint8_t>(), "order (default = 16)");

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

        if (vm.count("order"))
            args->order = vm["order"].as<uint8_t>();
        if (vm.count("use-target"))
            args->is_source = false;
    } catch (po::error &e) {
        std::cerr << "ERROR: " << e.what() << std::endl << std::endl;
        std::cerr << desc << std::endl;
        return false;
    }

    return true;
}

NGramTable LoadTable(const args_t &args) {
    vector<BilingualCorpus> corpora;
    BilingualCorpus::List(args.input_path, args.source_lang, args.target_lang, corpora);

    cout << "Loading domain... " << flush;
    NGramTable nGramTable(args.order);
    for (auto corpus = corpora.begin(); corpus != corpora.end(); ++corpus)
        nGramTable.Load(*corpus, args.is_source);
    cout << "DONE" << endl;

    return nGramTable;
}

// ------ Testing

bool RunTest(bool isSource, SuffixArray &index, const unordered_map<vector<wid_t>, size_t, phrase_hash> &ngrams) {
    for (auto entry = ngrams.begin(); entry != ngrams.end(); ++entry) {
        const vector<wid_t> &phrase = entry->first;
        size_t expectedCount = entry->second;

        size_t count = index.CountOccurrences(isSource, phrase);

        if (count != expectedCount) {
            cout << "CountSamples::FAILED (expected = " << expectedCount << " but found " << count << ")" << endl;
            return false;
        }
    }

    cout << "SUCCESS" << endl;

    return true;
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
    SuffixArray index(args.model_path, options.prefix_length);

    NGramTable nGramTable = LoadTable(args);
    for (uint8_t i = 1; i <= args.order; ++i) {
        unordered_map<vector<wid_t>, size_t, phrase_hash> ngrams = nGramTable.GetNGrams(i);

        cout << "Testing " << ((int) i) << "-grams (" << ngrams.size() << "):" << endl;
        if (!RunTest(args.is_source, index, ngrams)) {
            exit(TEST_FAILED);
        }
    }

    return SUCCESS;
}