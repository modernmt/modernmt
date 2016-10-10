//
// Created by Davide Caroselli on 03/10/16.
//

#include "PrefixCursor.h"
#include "dbkv.h"

using namespace mmt;
using namespace mmt::sapt;
using namespace rocksdb;

namespace mmt {
    namespace sapt {

        class DomainCursor : public PrefixCursor {
        public:

            DomainCursor(rocksdb::DB *db, length_t prefixLength, bool sourceSide, domain_t domain)
                    : db(db), sourceSide(sourceSide), domain(domain), prefixLength(prefixLength) {
            }

            virtual void Seek(const vector<wid_t> &phrase, size_t offset, size_t length) override {
                string key = MakePrefixKey(prefixLength, sourceSide, domain, phrase, offset, length);
                db->Get(ReadOptions(), key, &value);
            }

            virtual bool HasNext() override {
                return value.size() > 0;
            }

            virtual void Next() override {
                value.resize(0);
            }

            virtual void CollectValue(PostingList *output) override {
                output->Append(domain, value);
            }

            virtual size_t CountValue() override {
                return value.size() / PostingList::kEntrySize;
            }

        private:
            rocksdb::DB *db;

            const bool sourceSide;
            const domain_t domain;
            const length_t prefixLength;

            string value;
        };

        class GlobalCursor : public PrefixCursor {
        public:
            GlobalCursor(rocksdb::DB *db, length_t prefixLength, bool sourceSide, unordered_set<domain_t> *_skipList)
                    : sourceSide(sourceSide), skipDomains(_skipList != NULL), prefixLength(prefixLength),
                      it(db->NewIterator(ReadOptions())) {
                if (_skipList)
                    skipList.insert(_skipList->begin(), _skipList->end());
            }

            virtual void Seek(const vector<wid_t> &phrase, size_t offset, size_t length) override {
                key = MakePrefixKey(prefixLength, sourceSide, 0, phrase, offset, length);
                key.resize(key.size() - sizeof(domain_t));

                it->Seek(key);
            }

            virtual bool HasNext() override {
                bool hasNext = false;

                Slice currentKey;
                while (it->Valid() && (currentKey = it->key()).starts_with(key)) {
                    domain = GetDomainFromKey(currentKey.data(), prefixLength);

                    if (!skipDomains || skipList.find(domain) == skipList.end()) {
                        hasNext = true;
                        break;
                    } else {
                        it->Next();
                    }
                }

                return hasNext;
            }

            virtual void Next() override {
                it->Next();
            }

            virtual void CollectValue(PostingList *output) override {
                Slice value = it->value();
                output->Append(domain, string(value.data(), value.size()));
            }

            virtual size_t CountValue() override {
                return it->value().size() / PostingList::kEntrySize;
            }

            virtual ~GlobalCursor() {
                delete it;
            }

        private:
            const bool sourceSide;
            const bool skipDomains;
            const length_t prefixLength;
            unordered_set<domain_t> skipList;

            Iterator *it;
            string key;
            domain_t domain;
        };
    }
}

PrefixCursor *PrefixCursor::NewDomainCursor(rocksdb::DB *db, length_t prefixLength, bool sourceSide, domain_t domain) {
    return new DomainCursor(db, prefixLength, sourceSide, domain);
}

PrefixCursor *PrefixCursor::NewGlobalCursor(rocksdb::DB *db, length_t prefixLength, bool sourceSide,
                                            const context_t *skipDomains) {
    unordered_set<domain_t> domains;
    if (skipDomains) {
        for (auto score = skipDomains->begin(); score != skipDomains->end(); ++score)
            domains.insert(score->domain);
    }

    return new GlobalCursor(db, prefixLength, sourceSide, skipDomains ? &domains : NULL);
}
