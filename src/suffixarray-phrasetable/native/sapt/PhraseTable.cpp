//
// Created by Davide  Caroselli on 27/09/16.
//

#include "PhraseTable.h"
#include "CorpusStorage.h"
#include "CorpusIndex.h"
#include <boost/filesystem.hpp>
#include <iostream>

namespace fs = boost::filesystem;

using namespace mmt;
using namespace mmt::sapt;

namespace mmt {
    namespace sapt {
        struct PhraseTable::pt_private {
            CorpusStorage *storage;
            CorpusIndex *index;
        };

        PhraseTable::PhraseTable(const string &modelPath, const Options &options) {
            fs::path modelDir(modelPath);

            if (!fs::is_directory(modelDir))
                throw invalid_argument("Invalid model path: " + modelPath);

            fs::path storageFile = fs::absolute(modelDir / fs::path("corpora.bin"));
            fs::path indexPath = fs::absolute(modelDir / fs::path("index"));

            self = new pt_private();
            self->index = new CorpusIndex(indexPath.string());
            self->storage = new CorpusStorage(storageFile.string());
        }

        PhraseTable::~PhraseTable() {
            delete self->index;
            delete self->storage;
            delete self;
        }

        void PhraseTable::Add(const updateid_t &id, const domain_t domain, const std::vector <wid_t> &source,
                              const std::vector <wid_t> &target, const alignment_t &alignment) {
            // TODO: stub implementation

            cout << self->storage->Append(source, target, alignment) << endl;
        }

        vector <updateid_t> PhraseTable::GetLatestUpdatesIdentifier() {
            // TODO: stub implementation
            return vector<updateid_t>();
        }


        void PhraseTable::NormalizeContextMap(context_t *context) {
            // TODO: stub implementation (do nothing)
            // keep correcg implementation from rockslm::AdaptiveLM
            return;
        }

    }
}