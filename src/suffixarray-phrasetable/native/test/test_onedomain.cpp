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

        domain_t domain;
        uint8_t order = 16;
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
            ("domain,d", po::value<domain_t>()->required(), "domain to test")
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
        args->domain = vm["domain"].as<domain_t>();

        if (vm.count("order"))
            args->order = vm["order"].as<uint8_t>();
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

bool VerifyIntegrity(const vector<sample_t> &samples, const vector<wid_t> &phrase) {
    for (auto sample = samples.begin(); sample != samples.end(); ++sample) {
        if (sample->offsets.empty())
            return false;

        for (auto start = sample->offsets.begin(); start != sample->offsets.end(); ++start) {
            for (size_t i = 0; i < phrase.size(); ++i) {
                size_t source_i = (*start) + i;
                if (source_i >= sample->source.size())
                    return false;

                if (sample->source[source_i] != phrase[i])
                    return false;
            }
        }
    }

    return true;
}

size_t CountSamples(const vector<sample_t> &samples) {
    size_t count = 0;
    for (auto sample = samples.begin(); sample != samples.end(); ++sample) {
        count += sample->offsets.size();
    }
    return count;
}

bool RunTest(domain_t domain, SuffixArray &index, const unordered_map<vector<wid_t>, size_t, phrase_hash> &ngrams) {
    context_t context;
    context.push_back(cscore_t(domain, 1.f));

    size_t ngramsCount = ngrams.size();
    size_t currentCount = 0;

    for (auto entry = ngrams.begin(); entry != ngrams.end(); ++entry) {
        const vector<wid_t> &phrase = entry->first;
        size_t expectedCount = entry->second;

        vector<sample_t> samples;
        index.GetRandomSamples(phrase, expectedCount + 100, samples, &context);

        if (!VerifyIntegrity(samples, phrase)) {
            cout << "VerifyIntegrity::FAILED - ";
            for (auto word = phrase.begin(); word != phrase.end(); ++word)
                cout << " " << *word;
            cout << endl;
            return false;
        }

        samples.erase(std::remove_if(samples.begin(), samples.end(),
                                     [domain](const sample_t &sample) {
                                         return sample.domain != domain;
                                     }), samples.end());

        size_t count = CountSamples(samples);
        if (count != expectedCount) {
            cout << "CountSamples::FAILED (expected " << expectedCount << " but found " << count << ") -";
            for (auto word = phrase.begin(); word != phrase.end(); ++word)
                cout << " " << *word;
            cout << endl;
            return false;
        }

        currentCount++;

        if (currentCount % (ngramsCount / 10) == 0)
            cout << "." << flush;
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
        if (!RunTest(args.domain, index, ngrams)) {
            exit(TEST_FAILED);
        }
    }

    return SUCCESS;
}