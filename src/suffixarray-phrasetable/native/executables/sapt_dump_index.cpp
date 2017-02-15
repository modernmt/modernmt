#include <iostream>

#include <mmt/sentence.h>
#include <sapt/PhraseTable.h>
#include <suffixarray/UpdateBatch.h>
#include <suffixarray/SuffixArray.h>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <fstream>

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

namespace {
    const size_t ERROR_IN_COMMAND_LINE = 1;
    const size_t GENERIC_ERROR = 2;
    const size_t SUCCESS = 0;

    struct args_t {
        string model_path;
        string dump_file = "/dev/stdout";
    };
} // namespace

namespace po = boost::program_options;
namespace fs = boost::filesystem;

bool ParseArgs(int argc, const char *argv[], args_t *args) {
    po::options_description desc("dump the content of the SuffixArray Phrase Table");
    desc.add_options()
            ("help,h", "print this help message")
            ("model,m", po::value<string>()->required(), "model path")
            ("dump,d", po::value<string>(),
             "output file where dump the content of the database (default is /dev/stdout)");

    po::variables_map vm;
    try {
        po::store(po::parse_command_line(argc, argv, desc), vm);

        if (vm.count("help")) {
            cout << desc << endl;
            return false;
        }

        po::notify(vm);

        args->model_path = vm["model"].as<string>();

        if (vm.count("dump"))
            args->dump_file = vm["dump"].as<string>();

    } catch (po::error &e) {
        cerr << "ERROR: " << e.what() << endl << endl;
        cerr << desc << endl;
        return false;
    }

    return true;
}

int main(int argc, const char *argv[]) {
    args_t args;

    if (!ParseArgs(argc, argv, &args))
        return ERROR_IN_COMMAND_LINE;

    Options options;
    SuffixArray index(args.model_path, options.prefix_length, options.gc_timeout, options.gc_buffer_size);
    cerr << "Model loaded" << endl;

    ofstream output(args.dump_file.c_str());

    output << "GLOBAL size " << index.GetStorage()->GetSize() << endl;

    IndexIterator *iterator = index.NewIterator();
    IndexIterator::IndexEntry entry;

    while (iterator->Next(&entry)) {
        if (entry.is_source) {
            output << "SOURCE";
            output << " domain " << entry.domain;
            output << " words ";
            for (auto w = entry.words.begin(); w != entry.words.end(); ++w) { output << *w << ","; }
            output << " count " << entry.count;
            output << " positions ";
            for (auto l = entry.positions.begin(); l != entry.positions.end(); ++l) {
                output << l->pointer << ":" << l->offset << ",";
            }
            output << endl;
        } else {
            output << "TARGET";
            output << " words ";
            for (auto w = entry.words.begin(); w != entry.words.end(); ++w) { output << *w << ","; }
            output << " count " << entry.count;
            output << endl;
        }
    }


    delete iterator;
    cerr << "Dump ended" << endl;

    return SUCCESS;
}
