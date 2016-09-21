#include "lm/builder/output.hh"
#include "lm/builder/pipeline.hh"
#include "lm/common/size_option.hh"
#include "lm/lm_exception.hh"
#include "lm/model.hh"
#include "lm/sizes.hh"

#include "util/file.hh"
#include "util/file_piece.hh"
#include "util/usage.hh"

#include <iostream>

#include <boost/program_options.hpp>
#include <boost/version.hpp>
#include <vector>
#include <algorithm>
#include <cstdlib>
#include <exception>
#include <iomanip>
#include <limits>
#include <cmath>
#include <cstdlib>


#ifdef WIN32
#include "util/getopt.hh"
#else
#include <unistd.h>
#endif

namespace {
    enum RestFunction {
        REST_MAX,   // Maximum of any score to the left
        REST_LOWER, // Use lower-order files given below.
    };
    enum WriteMethod {
        WRITE_MMAP, // Map the file directly.
        WRITE_AFTER // Write after we're done.
    };

    enum WarningAction {
        THROW_UP,
        COMPLAIN,
        SILENT
    };

// Parse and validate pruning thresholds then return vector of threshold counts
// for each n-grams order.
    std::vector<uint64_t> ParsePruning(const std::vector<std::string> &param, std::size_t order) {
        // convert to vector of integers
        std::vector<uint64_t> prune_thresholds;
        prune_thresholds.reserve(order);
        for (std::vector<std::string>::const_iterator it(param.begin()); it != param.end(); ++it) {
            try {
                prune_thresholds.push_back(boost::lexical_cast<uint64_t>(*it));
            } catch(const boost::bad_lexical_cast &) {
                UTIL_THROW(util::Exception, "Bad pruning threshold " << *it);
            }
        }

        // Fill with zeros by default.
        if (prune_thresholds.empty()) {
            prune_thresholds.resize(order, 0);
            return prune_thresholds;
        }

        // validate pruning threshold if specified
        // throw if each n-gram order has not  threshold specified
        UTIL_THROW_IF(prune_thresholds.size() > order, util::Exception, "You specified pruning thresholds for orders 1 through " << prune_thresholds.size() << " but the model only has order " << order);
        // threshold for unigram can only be 0 (no pruning)

        // check if threshold are not in decreasing order
        uint64_t lower_threshold = 0;
        for (std::vector<uint64_t>::iterator it = prune_thresholds.begin(); it != prune_thresholds.end(); ++it) {
            UTIL_THROW_IF(lower_threshold > *it, util::Exception, "Pruning thresholds should be in non-decreasing order.  Otherwise substrings would be removed, which is bad for query-time data structures.");
            lower_threshold = *it;
        }

        // Pad to all orders using the last value.
        prune_thresholds.resize(order, prune_thresholds.back());
        return prune_thresholds;
    }

    lm::builder::Discount ParseDiscountFallback(const std::vector<std::string> &param) {
        lm::builder::Discount ret;
        UTIL_THROW_IF(param.size() > 3, util::Exception, "Specify at most three fallback discounts: 1, 2, and 3+");
        UTIL_THROW_IF(param.empty(), util::Exception, "Fallback discounting enabled, but no discount specified");
        ret.amount[0] = 0.0;
        for (unsigned i = 0; i < 3; ++i) {
            float discount = boost::lexical_cast<float>(param[i < param.size() ? i : (param.size() - 1)]);
            UTIL_THROW_IF(discount < 0.0 || discount > static_cast<float>(i+1), util::Exception, "The discount for count " << (i+1) << " was parsed as " << discount << " which is not in the range [0, " << (i+1) << "].");
            ret.amount[i + 1] = discount;
        }
        return ret;
    }

} // namespace


namespace lm {
    namespace ngram {
        namespace {


            void Usage(const char *name, const char *default_mem) {
                //TODO: to revise totally
                std::cerr << "Usage: " << name << " --text input.arpa --model output.mmap [-u log10_unknown_probability] [-s] [-i] [-w mmap|after] [-p probing_multiplier] [-T trie_temporary] [-S trie_building_mem] [-q bits] [-b bits] [-a bits] [--type type]\n\n"
                        "-u sets the log10 probability for <unk> if the ARPA file does not have one.\n"
                        "   Default is -100.  The ARPA file will always take precedence.\n"
                        "-s allows models to be built even if they do not have <s> and </s>.\n"
                        "-i allows buggy models from IRSTLM by mapping positive log probability to 0.\n"
                        "-w mmap|after determines how writing is done.\n"
                        "   mmap maps the binary file and writes to it.  Default for trie.\n"
                        "   after allocates anonymous memory, builds, and writes.  Default for probing.\n"
                        "-r \"order1.arpa order2 order3 order4\" adds lower-order rest costs from these\n"
                        "   model files.  order1.arpa must be an ARPA file.  All others may be ARPA or\n"
                        "   the same data structure as being built.  All files must have the same\n"
                        "   vocabulary.  For probing, the unigrams must be in the same order.\n\n"
                        "type is either probing or trie.  Default is probing.\n\n"
                        "probing uses a probing hash table.  It is the fastest but uses the most memory.\n"
                        "-p sets the space multiplier and must be >1.0.  The default is 1.5.\n\n"
                        "trie is a straightforward trie with bit-level packing.  It uses the least\n"
                        "memory and is still faster than SRI or IRST.  Building the trie format uses an\n"
                        "on-disk sort to save memory.\n"
                        "-T is the temporary directory prefix.  Default is the output file name.\n"
                        "-S determines memory use for sorting.  Default is " << default_mem << ".  This is compatible\n"
                        "   with GNU sort.  The number is followed by a unit: \% for percent of physical\n"
                        "   memory, b for bytes, K for Kilobytes, M for megabytes, then G,T,P,E,Z,Y.  \n"
                        "   Default unit is K for Kilobytes.\n"
                        "-q turns quantization on and sets the number of bits (e.g. -q 8).\n"
                        "-b sets backoff quantization bits.  Requires -q and defaults to that value.\n"
                        "-a compresses pointers using an array of offsets.  The parameter is the\n"
                        "   maximum number of bits encoded by the array.  Memory is minimized subject\n"
                        "   to the maximum, so pick 255 to minimize memory.\n\n"
                        "-h print this help message.\n\n"
                        "Get a memory estimate by passing an ARPA file without an output file name.\n";
                exit(1);
            }

// I could really use boost::lexical_cast right about now.
            float ParseFloat(const char *from) {
                char *end;
                float ret = strtod(from, &end);
                if (*end) throw util::ParseNumberException(from);
                return ret;
            }
            unsigned long int ParseUInt(const char *from) {
                char *end;
                unsigned long int ret = strtoul(from, &end, 10);
                if (*end) throw util::ParseNumberException(from);
                return ret;
            }

            uint8_t ParseBitCount(const char *from) {
                unsigned long val = ParseUInt(from);
                if (val > 25) {
                    util::ParseNumberException e(from);
                    e << " bit counts are limited to 25.";
                }
                return val;
            }

            void ParseFileList(const char *from, std::vector<std::string> &to) {
                to.clear();
                while (true) {
                    const char *i;
                    for (i = from; *i && *i != ' '; ++i) {}
                    to.push_back(std::string(from, i - from));
                    if (!*i) break;
                    from = i + 1;
                }
            }

            void ProbingQuantizationUnsupported() {
                std::cerr << "Quantization is only implemented in the trie data structure." << std::endl;
                exit(1);
            }

        } // namespace ngram
    } // namespace lm
} // namespace


int main(int argc, char *argv[]) {

    try {

        using namespace lm::ngram;

        const char *default_mem = util::GuessPhysicalMemory() ? "80%" : "1G";

        bool quantize = false;
        bool set_backoff_bits = false;
        bool bhiksha = false;
        bool set_write_method = false;
        bool rest = false;
        lm::ngram::Config config;

        namespace po = boost::program_options;
        po::options_description options("Language model building options");
        lm::builder::PipelineConfig pipeline;

        std::string text, model, model_type;
        std::string temporary_directory;
        std::vector<std::string> pruning;
        std::vector<std::string> discount_fallback;
        std::vector<std::string> discount_fallback_default;
        discount_fallback_default.push_back("0.5");
        discount_fallback_default.push_back("1");
        discount_fallback_default.push_back("1.5");
        bool verbose_header;

        // Read from stdinby default
        util::scoped_fd in(0);
        // Write to stdout by default
        util::scoped_fd out(1);  //todo: remove default aoutput

        std::string rest_string, write_method;

        options.add_options()
                ("help,h", po::bool_switch(), "Show this help message")
                ("order,o", po::value<std::size_t>(&pipeline.order)
#if BOOST_VERSION >= 104200
                         ->required()
#endif
                , "Order of the model")
                ("interpolate_unigrams",
                 po::value<bool>(&pipeline.initial_probs.interpolate_unigrams)->default_value(true)->implicit_value(
                         true),
                 "Interpolate the unigrams (default) as opposed to giving lots of mass to <unk> like SRI.  If you want SRI's behavior with a large <unk> and the old lmplz default, use --interpolate_unigrams 0.")
                ("skip_symbols", po::bool_switch(),
                 "Treat <s>, </s>, and <unk> as whitespace instead of throwing an exception")
                ("temp_directory,T",
                 po::value<std::string>(&temporary_directory)->default_value(util::DefaultTempDirectory()),
                 "Temporary directory for internal computation. The directory must exist. ")
                ("memory,S", lm::SizeOption(pipeline.sort.total_memory, util::GuessPhysicalMemory() ? "80%" : "1G"),
                 "Sorting memory     determines memory use for sorting.  Default is 80\%.  This is compatible with GNU sort.  The number is followed by a unit: \% for percent of physical memory, b for bytes, K for Kilobytes, M for megabytes, then G,T,P,E,Z,Y. Default unit is K for Kilobytes.")
                ("minimum_block", lm::SizeOption(pipeline.minimum_block, "8K"), "Minimum block size to allow")
                ("sort_block", lm::SizeOption(pipeline.sort.buffer_size, "64M"),
                 "Size of IO operations for sort (determines arity)")
                ("block_count", po::value<std::size_t>(&pipeline.block_count)->default_value(2),
                 "Block count (per order)")
                ("vocab_estimate", po::value<lm::WordIndex>(&pipeline.vocab_estimate)->default_value(1000000),
                 "Assume this vocabulary size for purposes of calculating memory in step 1 (corpus count) and pre-sizing the hash table")
                ("vocab_pad", po::value<uint64_t>(&pipeline.vocab_size_for_unk)->default_value(0),
                 "If the vocabulary is smaller than this value, pad with <unk> to reach this size. Requires --interpolate_unigrams")
                ("verbose_header", po::bool_switch(&verbose_header),
                 "Add a verbose header to the ARPA file that includes information such as token count, smoothing type, etc.")
                ("text", po::value<std::string>(&text), "Read text from a file instead of stdin")
                ("model", po::value<std::string>(&model), "File with the estimated model")
                ("type", po::value<std::string>(&model_type), "Model type (probing, trie, ...) probing by default.")
                ("renumber", po::bool_switch(&pipeline.renumber_vocabulary),
                 "Rrenumber the vocabulary identifiers so that they are monotone with the hash of each string.  This is consistent with the ordering used by the trie data structure.")
                ("collapse_values", po::bool_switch(&pipeline.output_q),
                 "Collapse probability and backoff into a single value, q that yields the same sentence-level probabilities.  See http://kheafield.com/professional/edinburgh/rest_paper.pdf for more details, including a proof.")
                ("prune", po::value<std::vector<std::string> >(&pruning)->multitoken(),
                 "Prune n-grams with count less than or equal to the given threshold.  Specify one value for each order i.e. 0 0 1 to prune singleton trigrams and above.  The sequence of values must be non-decreasing and the last value applies to any remaining orders. Default is to not prune, which is equivalent to --prune 0.")
                ("limit_vocab_file", po::value<std::string>(&pipeline.prune_vocab_file)->default_value(""),
                 "Read allowed vocabulary separated by whitespace. N-grams that contain vocabulary items not in this list will be pruned. Can be combined with --prune arg")
                ("discount_fallback",
                 po::value<std::vector<std::string> >(&discount_fallback)->multitoken()->implicit_value(
                         discount_fallback_default, "0.5 1 1.5"),
                 "The closed-form estimate for Kneser-Ney discounts does not work without singletons or doubletons.  It can also fail if these values are out of range.  This option falls back to user-specified discounts when the closed-form estimate fails.  Note that this option is generally a bad idea: you should deduplicate your corpus instead.  However, class-based models need custom discounts because they lack singleton unigrams.  Provide up to three discounts (for adjusted counts 1, 2, and 3+), which will be applied to all orders where the closed-form estimates fail.")
                ("u", po::value<float>(&config.unknown_missing_logprob), "sets the log10 probability for <unk> if the ARPA file does not have one. Default is -100.  The ARPA file will always take precedence.")
                ("s", po::bool_switch(), "allows models to be built even if they do not have <s> and </s>.")
                ("i", po::bool_switch(), "allows buggy models from IRSTLM by mapping positive log probability to 0.")
                ("w", po::value<std::string>(&write_method),
                 "mmap|after determines how writing is done. mmap maps the binary file and writes to it.  Default for trie. after allocates anonymous memory, builds, and writes.  Default for probing.")
                ("r", po::value<std::string>(&rest_string),
                 "\"order1.arpa order2 order3 order4\" adds lower-order rest costs from these  model files.  order1.arpa must be an ARPA file.  All others may be ARPA or the same data structure as being built.  All files must have the same vocabulary.  For probing, the unigrams must be in the same order. type is either probing or trie.  Default is probing. probing uses a probing hash table.  It is the fastest but uses the most memory.")
                ("p", po::value<float>(&config.probing_multiplier),
                 "sets the space multiplier and must be >1.0.  The default is 1.5. trie is a straightforward trie with bit-level packing.  It uses the least memory and is still faster than SRI or IRST.  Building the trie format uses an on-disk sort to save memory.")
                ("q", po::value<uint8_t>(&config.prob_bits), "turns quantization on and sets the number of bits (e.g. -q 8).")
                ("b", po::value<uint8_t>(&config.backoff_bits), "sets backoff quantization bits.  Requires -q and defaults to that value.")
                ("a", po::value<uint8_t>(&config.pointer_bhiksha_bits),
                 "compresses pointers using an array of offsets.  The parameter is the maximum number of bits encoded by the array.  Memory is minimized subject to the maximum, so pick 255 to minimize memory.");


        po::variables_map vm;
        po::store(po::parse_command_line(argc, argv, options), vm);

        if (argc == 1 || vm["help"].as<bool>()) {
            std::cerr << "Builds unpruned language models with modified Kneser-Ney smoothing.\n\n"
            << "Usage: " << argv[0]
            << " --text input.arpa --model output.mmap [-u log10_unknown_probability] [-s] [-i] [-w mmap|after] [-p probing_multiplier] [-T trie_temporary] [-S trie_building_mem] [-q bits] [-b bits] [-a bits] [--type type]\n\n";
            uint64_t mem = util::GuessPhysicalMemory();
            if (mem) {
                std::cerr << "This machine has " << mem << " bytes of memory.\n\n";
            } else {
                std::cerr << "Unable to determine the amount of memory on this machine.\n\n";
            }
            std::cerr << options << std::endl;
            return 1;
        }

        po::notify(vm);

        // required() appeared in Boost 1.42.0.
#if BOOST_VERSION < 104200
        if (!vm.count("order")) {
      std::cerr << "the option '--order' is required but missing" << std::endl;
      return 1;
    }
#endif

//setting parameter for both lmplz and build_binary

        if (vm.count("T")) {
            temporary_directory = vm["T"].as<std::string>();
            util::NormalizeTempPrefix(temporary_directory);
        }

        if (vm.count("temp_directory")) {
            temporary_directory = vm["temp_directory"].as<std::string>();
            util::NormalizeTempPrefix(temporary_directory);
        }

        std::string temporary_arpa = temporary_directory + "/lm.arpa";
        std::cerr << "temporary_directory:" << temporary_directory << std::endl;
        std::cerr << "temporary_arpa:" << temporary_arpa << std::endl;

//setting parameter for lmplz
        if (pipeline.vocab_size_for_unk && !pipeline.initial_probs.interpolate_unigrams) {
            std::cerr << "--vocab_pad requires --interpolate_unigrams be on" << std::endl;
            return 1;
        }

        if (vm["skip_symbols"].as<bool>()) {
            pipeline.disallowed_symbol_action = lm::COMPLAIN;
        } else {
            pipeline.disallowed_symbol_action = lm::THROW_UP;
        }

        if (vm.count("discount_fallback")) {
            pipeline.discount.fallback = ParseDiscountFallback(discount_fallback);
            pipeline.discount.bad_action = lm::COMPLAIN;
        } else {
            // Unused, just here to prevent the compiler from complaining about uninitialized.
            pipeline.discount.fallback = lm::builder::Discount();
            pipeline.discount.bad_action = lm::THROW_UP;
        }

        // parse pruning thresholds.  These depend on order, so it is not done as a notifier.
        pipeline.prune_thresholds = ParsePruning(pruning, pipeline.order);

        if (!vm["limit_vocab_file"].as<std::string>().empty()) {
            pipeline.prune_vocab = true;
        }
        else {
            pipeline.prune_vocab = false;
        }

        if (vm.count("text")) {
            in.reset(util::OpenReadOrThrow(vm["text"].as<std::string>().c_str()));
        }


        lm::builder::InitialProbabilitiesConfig &initial = pipeline.initial_probs;
        // TODO: evaluate options for these.
        initial.adder_in.total_memory = 32768;
        initial.adder_in.block_count = 2;
        initial.adder_out.total_memory = 32768;
        initial.adder_out.block_count = 2;
        pipeline.read_backoffs = initial.adder_out;


//setting parameter for build_binary
        config.building_memory = util::ParseSize(default_mem);

        if (vm.count("q")) {
            config.prob_bits = vm["q"].as<uint8_t>();
            if (!set_backoff_bits) config.backoff_bits = config.prob_bits;
            quantize = true;
        }
        if (vm.count("b")) {
            config.prob_bits = vm["b"].as<uint8_t>();
            set_backoff_bits = true;
        }
        if (vm.count("a")) {
            config.pointer_bhiksha_bits = vm["a"].as<uint8_t>();
            bhiksha = true;
        }
        if (vm.count("u")) {
            config.unknown_missing_logprob = vm["u"].as<float>();
        }
        if (vm.count("p")) {
            config.probing_multiplier = vm["p"].as<float>();
        }
        if (vm.count("S")) {
            config.building_memory = std::min(std::numeric_limits<std::size_t>::max(), vm["S"].as<size_t>());
        }
        if (vm.count("s")) {
            config.sentence_marker_missing = lm::SILENT;
        }
        if (vm.count("w")) {
            set_write_method = true;
            if (vm["w"].as<std::string>() == "mmap") {
                config.write_method = Config::WRITE_MMAP;
            } else if (vm["w"].as<std::string>() == "after") {
                config.write_method = Config::WRITE_AFTER;
            } else {
                Usage(argv[0], default_mem);
            }
        }
        if (vm.count("r")) {
            rest = true;
            ParseFileList(rest_string.c_str(), config.rest_lower_files);
            config.rest_function = Config::REST_LOWER;
        }

        model_type = "probing";
        if (vm.count("type")) {
            model_type = vm["type"].as<std::string>();
        }
        //it is mandatory to specify an output file
        if (vm.count("model")) {
            config.write_mmap = vm["model"].as<std::string>().c_str();
        } else {
            std::cerr << "it is mandatory to specify an output file (--model <file>)" << std::endl;
            exit(1);
        }




        //estimation of LM and output into a temporary arpa file
        try {
            out.reset(util::CreateOrThrow(temporary_arpa.c_str()));
            lm::builder::Output output(temporary_directory, false, pipeline.output_q);

            output.Add(new lm::builder::PrintHook(out.release(), verbose_header));

            lm::builder::Pipeline(pipeline, in.release(), output);
        } catch (const util::MallocException &e) {
            std::cerr << e.what() << std::endl;
            std::cerr << "Try rerunning with a more conservative -S setting than " << vm["memory"].as<std::string>() <<
            std::endl;
            return 1;
        }
        util::PrintUsage(std::cerr);


        //building the binary LM from the temporary arpa file
        try {
            if (!quantize && set_backoff_bits) {
                std::cerr << "You specified backoff quantization (-b) but not probability quantization (-q)" <<
                std::endl;
                abort();
            }

//todo: set from_file and model_type properly

            if (model_type == "probing") {
                if (!set_write_method) config.write_method = Config::WRITE_AFTER;
                if (quantize || set_backoff_bits) ProbingQuantizationUnsupported();
                if (rest) {
                    RestProbingModel(temporary_arpa.c_str(), config);
                } else {
                    ProbingModel(temporary_arpa.c_str(), config);
                }
            } else if (model_type == "trie") {
                if (rest) {
                    std::cerr << "Rest + trie is not supported yet." << std::endl;
                    return 1;
                }
                if (!set_write_method) config.write_method = Config::WRITE_MMAP;
                if (quantize) {
                    if (bhiksha) {
                        QuantArrayTrieModel(temporary_arpa.c_str(), config);
                    } else {
                        QuantTrieModel(temporary_arpa.c_str(), config);
                    }
                } else {
                    if (bhiksha) {
                        ArrayTrieModel(temporary_arpa.c_str(), config);
                        ArrayTrieModel(temporary_arpa.c_str(), config);
                    } else {
                        TrieModel(temporary_arpa.c_str(), config);
                    }
                }
            } else {
                Usage(argv[0], default_mem);
            }
        } catch (const std::exception &e) {
            std::cerr << e.what() << std::endl;
            std::cerr << "ERROR of build_binary" << std::endl;
            return 1;
        }
    } catch (const std::exception &e) {
        std::cerr << e.what() << std::endl;
        std::cerr << "ERROR global error " << std::endl;
        return 1;
    }

    std::cerr << "SUCCESS" << std::endl;
    return 0;
}