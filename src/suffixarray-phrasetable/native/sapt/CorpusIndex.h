//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_CORPUSINDEX_H
#define SAPT_CORPUSINDEX_H

#include <string>
#include <rocksdb/db.h>

using namespace std;

namespace mmt {
    namespace sapt {

        class CorpusIndex {
        public:
            CorpusIndex(const string &path, bool prepareForBulkLoad = false){};

            ~CorpusIndex(){};

        private:
            rocksdb::DB *db;
        };

    }
}


#endif //SAPT_CORPUSINDEX_H
