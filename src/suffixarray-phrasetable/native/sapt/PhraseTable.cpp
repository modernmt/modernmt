//
// Created by Davide  Caroselli on 27/09/16.
//

#include <suffixarray/SuffixArray.h>
#include "PhraseTable.h"
#include "UpdateManager.h"

using namespace mmt;
using namespace mmt::sapt;

struct PhraseTable::pt_private {
    SuffixArray *index;
    UpdateManager *updates;
};

PhraseTable::PhraseTable(const string &modelPath, const Options &options) {
    self = new pt_private();
    self->index = new SuffixArray(modelPath, options.prefix_length, options.max_option_length);
    self->updates = new UpdateManager(self->index, options.update_buffer_size, options.update_max_delay);
}

PhraseTable::~PhraseTable() {
    delete self->updates;
    delete self->index;
    delete self;
}

void PhraseTable::Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                      const std::vector<wid_t> &target, const alignment_t &alignment) {
    self->updates->Add(id, domain, source, target, alignment);
}

vector<updateid_t> PhraseTable::GetLatestUpdatesIdentifier() {
    const vector<seqid_t> &streams = self->index->GetStreams();

    vector<updateid_t> result;
    result.reserve(streams.size());

    for (size_t i = 0; i < streams.size(); ++i) {
        if (streams[i] != 0)
            result.push_back(updateid_t((stream_t) i, streams[i]));

    }

    return result;
}

void *PhraseTable::__GetSuffixArray() {
    return self->index;
}
