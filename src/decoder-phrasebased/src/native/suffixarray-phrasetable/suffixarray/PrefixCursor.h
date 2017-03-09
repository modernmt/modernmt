//
// Created by Davide Caroselli on 03/10/16.
//

#ifndef SAPT_PREFIXCURSOR_H
#define SAPT_PREFIXCURSOR_H

#include <rocksdb/db.h>
#include <mmt/sentence.h>
#include "PostingList.h"

using namespace std;

namespace mmt {
    namespace sapt {

        class PrefixCursor {
        public:

            static PrefixCursor *NewDomainCursor(rocksdb::DB *db, length_t prefixLength, domain_t domain);

            static PrefixCursor *NewGlobalCursor(rocksdb::DB *db, length_t prefixLength,
                                                 const context_t *skipDomains = NULL);

            virtual ~PrefixCursor() {};

            void Seek(const vector<wid_t> &phrase) {
                Seek(phrase, 0, phrase.size());
            }

            virtual void Seek(const vector<wid_t> &phrase, size_t offset, size_t length) = 0;

            virtual bool HasNext() = 0;

            virtual void Next() = 0;

            virtual void CollectValue(PostingList *output) = 0;

            virtual size_t CountValue() = 0;
        };

    }
}


#endif //SAPT_PREFIXCURSOR_H
