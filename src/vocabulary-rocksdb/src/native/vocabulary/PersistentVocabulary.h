//
// Created by Davide  Caroselli on 17/08/16.
//

#ifndef MMTCORE_PERSISTENTVOCABULARY_H
#define MMTCORE_PERSISTENTVOCABULARY_H

#include <mmt/vocabulary/Vocabulary.h>
#include <string>
#include <rocksdb/db.h>
#include <unordered_set>
#include "IdGenerator.h"

using namespace std;

namespace mmt {
    namespace vocabulary {

        class PersistentVocabulary : public Vocabulary {
        public:
            PersistentVocabulary(string path, bool prepareForBulkLoad = false);

            virtual ~PersistentVocabulary() override;

            virtual wid_t Lookup(const string &word, bool putIfAbsent) override;

            virtual void
            Lookup(const vector<vector<string>> &buffer, vector<vector<wid_t>> *output, bool putIfAbsent) override;

            virtual const bool ReverseLookup(wid_t id, string *output) override;

            virtual const bool ReverseLookup(const vector<vector<wid_t>> &buffer, vector<vector<string>> &output) override;

            void Put(const string &word, const wid_t id);

            void ForceCompaction();

            void ResetId(wid_t id);

        private:
            string idGeneratorPath;
            IdGenerator idGenerator;
            rocksdb::DB* directDb;
            rocksdb::DB* reverseDb;
        };

    }
}


#endif //MMTCORE_PERSISTENTVOCABULARY_H
