//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_PHRASETABLE_H
#define SAPT_PHRASETABLE_H

#include <iostream>
#include <sstream>
#include <map>

#include <mmt/IncrementalModel.h>
#include <mmt/aligner/Aligner.h>
#include "Options.h"
#include "TranslationOption.h"

using namespace std;

namespace mmt {
    namespace sapt {

        struct ptphrase_hash {
            size_t operator()(const vector<wid_t> &x) const {
                size_t hash = 1;
                for (size_t i = 0; i < x.size(); ++i)
                    hash = 31 * hash + x[i];

                return hash;
            }
        };

        typedef unordered_map<vector<wid_t>, vector<TranslationOption>, ptphrase_hash> translation_table_t;

        class PhraseTable : public IncrementalModel {
        public:
            PhraseTable(const string &modelPath, const Options &options = Options(), Aligner *aligner = NULL);

            ~PhraseTable();

            vector<TranslationOption> GetTranslationOptions(const vector<wid_t> &phrase, context_t *context = NULL);

            translation_table_t GetAllTranslationOptions(const vector<wid_t> &sentence, context_t *context = NULL);

            /* IncrementalModel */

            virtual void Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                             const std::vector<wid_t> &target, const alignment_t &alignment) override;

            virtual vector<updateid_t> GetLatestUpdatesIdentifier() override;

        private:
            struct pt_private;
            pt_private *self;
        };
    }
}


#endif //SAPT_PHRASETABLE_H
