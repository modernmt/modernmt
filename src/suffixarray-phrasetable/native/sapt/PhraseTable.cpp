//
// Created by Davide  Caroselli on 27/09/16.
//

#include "PhraseTable.h"
#include "CorpusStorage.h"
#include "CorpusIndex.h"
#include "UpdateManager.h"
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
            UpdateManager *updates;
        };


        PhraseTable::PhraseTable(const string &modelPath, const Options &options) {
            fs::path modelDir(modelPath);

            if (!fs::is_directory(modelDir))
                throw invalid_argument("Invalid model path: " + modelPath);

            fs::path storageFile = fs::absolute(modelDir / fs::path("corpora.bin"));
            fs::path indexPath = fs::absolute(modelDir / fs::path("index"));

            self = new pt_private();
            self->index = new CorpusIndex(indexPath.string(), options.prefix_length);
            self->storage = new CorpusStorage(storageFile.string(), self->index->GetStorageSize());
            self->updates = new UpdateManager(self->storage, self->index, options.update_buffer_size,
                                              options.update_max_delay);
        }

        PhraseTable::~PhraseTable() {
            delete self->updates;
            delete self->index;
            delete self->storage;
            delete self;
        }

        void PhraseTable::Add(const updateid_t &id, const domain_t domain, const std::vector <wid_t> &source,
                              const std::vector <wid_t> &target, const alignment_t &alignment) {
            self->updates->Add(id, domain, source, target, alignment);
        }

        vector <updateid_t> PhraseTable::GetLatestUpdatesIdentifier() {
            const vector <seqid_t> &streams = self->index->GetStreamsStatus();

            vector <updateid_t> result;
            result.reserve(streams.size());

            for (size_t i = 0; i < streams.size(); ++i) {
                if (streams[i] != 0)
                    result.push_back(updateid_t((stream_t) i, streams[i]));

            }

            return result;
        }

        void PhraseTable::NormalizeContextMap(context_t *context) {
            // TODO: stub implementation (do nothing)
            // keep correcg implementation from rockslm::AdaptiveLM
            return;
        }
    }
}
