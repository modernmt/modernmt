//
// Created by Davide  Caroselli on 08/09/16.
//

#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <lm/InterpolatedLM.h>
#include <iostream>
#include <corpus/CorpusReader.h>

using namespace std;
using namespace mmt;
using namespace mmt::ilm;

namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t SUCCESS = 0;

    struct args_t {
        bool alm_only = false;
        bool slm_only = false;
        context_t context_map;
        string model_path;
        string vocabulary_path;
        uint8_t order = 5;
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
        memory_t id = (memory_t) stoi(tok);
        getline(ess, tok, ':');
        float w = stof(tok);

        cscore_t entry(id, w);
        context.push_back(entry);
    }

    return true;
}

#define PrintUsage(name) {cerr << "USAGE: " << name << " [-h] [--alm-only|--slm-only] [-o ARG] [-c ARG] MODEL_PATH" << endl << endl;}

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    string appName = fs::basename(argv[0]);

    po::options_description options("Option arguments");
    options.add_options()
            ("help,h", "print this help message")
            ("model", po::value<string>()->required(), "InterpolatedLM model path")
            ("vocabulary,v", po::value<string>()->required(), "vocabulary file")
            ("context,c", po::value<string>(), "context map in the format <id>:<w>[,<id>:<w>]")
            ("alm-only", "use AdaptiveLM only")
            ("slm-only", "use StaticLM only")
            ("order,o", po::value<uint8_t>(), "the language model order (default is 5)");

    po::positional_options_description pOptions;
    pOptions.add("model", 1);

    po::variables_map vm;
    try {
        po::store(po::command_line_parser(argc, argv).options(options).positional(pOptions).run(), vm);

        if (vm.count("help")) {
            cerr << "InterpolatedLM is an incremental, adaptive language model. Use this binary "
                    "to calculate perplexity of an input document. The document must have been preprocessed "
                    "and it must be created with the same vocabulary used during training." << endl << endl;

            PrintUsage(appName);
            cerr << options << endl << endl;
            return false;
        }

        po::notify(vm);

        args->model_path = vm["model"].as<string>();
        args->vocabulary_path = vm["vocabulary"].as<string>();

        if (vm.count("context")) {
            if (!ParseContextMap(vm["context"].as<string>(), args->context_map))
                throw po::error("invalid context map: " + vm["context"].as<string>());
        }

        args->alm_only = vm.count("alm-only") > 0;
        args->slm_only = vm.count("slm-only") > 0;
        if (vm.count("order"))
            args->order = vm["order"].as<uint8_t>();

        if (args->alm_only && args->slm_only)
            throw po::error("invalid options, you cannot specify both --alm-only and --slm-only");
    } catch (po::error &e) {
        cerr << "ERROR: " << e.what() << endl << endl;
        PrintUsage(appName);
        cerr << options << endl;
        return false;
    }

    return true;
}

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    Options options;

    options.static_lm.type = Options::StaticLMType::TRIE;
    options.static_lm.quantization_bits = (uint8_t) 8;
    options.static_lm.pointers_compression_bits = (uint8_t) 32;
    options.order = args.order;
    if (args.alm_only)
        options.adaptivity_ratio = 1.f;
    if (args.slm_only)
        options.adaptivity_ratio = 0.f;

    Vocabulary vb(args.vocabulary_path, true);
    InterpolatedLM lm(args.model_path, options);
    cerr << "Model loaded." << endl;

    CorpusReader reader(vb, &cin);
    vector<wid_t> line;

    size_t word_count = 0;
    float corpusProbability = 0.f;

    vector<wid_t> sentenceBegin(1);
    sentenceBegin.push_back(kVocabularyStartSymbol);

    while (reader.Read(line)) {
        line.push_back(kVocabularyEndSymbol);

        HistoryKey *historyKey = lm.MakeHistoryKey(sentenceBegin);
        float sentenceProbability = 0.0;

        for (auto word = line.begin(); word != line.end(); ++word) {
            HistoryKey *outKey;

            float wordProbability = lm.ComputeProbability(*word, historyKey, &args.context_map, &outKey);

            delete historyKey;
            historyKey = outKey;

            cout << *word
                  << (lm.IsOOV(&args.context_map, *word) ? "[OOV] " : "") << " "
                 << "Length: " << historyKey->length() << " "
                 << wordProbability << "\t";

            sentenceProbability += wordProbability;
            ++word_count;
        }

        corpusProbability += sentenceProbability;

        delete historyKey;
        cout << endl;
    }

    double perplexity = (double) exp(-(corpusProbability / static_cast<double>(word_count)));

    cout << endl;
    cout << "Perplexity: " << perplexity << endl;
    cout << "Corpus probability: " << corpusProbability << endl;

    return SUCCESS;
}
