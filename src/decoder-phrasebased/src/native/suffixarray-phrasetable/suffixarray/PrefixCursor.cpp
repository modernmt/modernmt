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

        class MemoryCursor : public PrefixCursor {
        public:

            MemoryCursor(rocksdb::DB *db, length_t prefixLength, memory_t memory)
                    : db(db), memory(memory), prefixLength(prefixLength) {
            }

            virtual void Seek(const vector<wid_t> &phrase, size_t offset, size_t length) override {
                string key = MakePrefixKey(prefixLength, memory, phrase, offset, length);
                db->Get(ReadOptions(), key, &value);
            }

            virtual bool HasNext() override {
                return value.size() > 0;
            }

            virtual void Next() override {
                value.resize(0);
            }

            virtual void CollectValue(PostingList *output) override {
                output->Append(memory, value);
            }

            virtual size_t CountValue() override {
                return value.size() / PostingList::kEntrySize;
            }

        private:
            rocksdb::DB *db;

            const memory_t memory;
            const length_t prefixLength;

            string value;
        };

        class GlobalCursor : public PrefixCursor {
        public:
            GlobalCursor(rocksdb::DB *db, length_t prefixLength, unordered_set<memory_t> *_skipList)
                    : skipMemories(_skipList != NULL), prefixLength(prefixLength), it(db->NewIterator(ReadOptions())) {
                if (_skipList)
                    skipList.insert(_skipList->begin(), _skipList->end());
            }

            virtual void Seek(const vector<wid_t> &phrase, size_t offset, size_t length) override {
                key = MakePrefixKey(prefixLength, 0, phrase, offset, length);
                key.resize(key.size() - sizeof(memory_t));

                it->Seek(key);
            }

            virtual bool HasNext() override {
                bool hasNext = false;

                Slice currentKey;
                while (it->Valid() && (currentKey = it->key()).starts_with(key)) {
                    memory = GetMemoryFromKey(currentKey.data(), prefixLength);

                    if (!skipMemories || skipList.find(memory) == skipList.end()) {
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
                output->Append(memory, string(value.data(), value.size()));
            }

            virtual size_t CountValue() override {
                return it->value().size() / PostingList::kEntrySize;
            }

            virtual ~GlobalCursor() {
                delete it;
            }

        private:
            const bool skipMemories;
            const length_t prefixLength;
            unordered_set<memory_t> skipList;

            Iterator *it;
            string key;
            memory_t memory;
        };
    }
}

PrefixCursor *PrefixCursor::NewMemoryCursor(rocksdb::DB *db, length_t prefixLength, memory_t memory) {
    return new MemoryCursor(db, prefixLength, memory);
}

PrefixCursor *PrefixCursor::NewGlobalCursor(rocksdb::DB *db, length_t prefixLength, const context_t *skipMemories) {
    unordered_set<memory_t> memories;
    if (skipMemories) {
        for (auto score = skipMemories->begin(); score != skipMemories->end(); ++score)
            memories.insert(score->memory);
    }

    return new GlobalCursor(db, prefixLength, skipMemories ? &memories : NULL);
}
