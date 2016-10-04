//
// Created by Davide  Caroselli on 28/09/16.
//

#ifndef SAPT_SUFFIXARRAY_H
#define SAPT_SUFFIXARRAY_H

#include <string>
#include <rocksdb/db.h>
#include <mmt/IncrementalModel.h>
#include <unordered_set>
#include <mutex>
#include "UpdateBatch.h"
#include "CorpusStorage.h"
#include "PostingList.h"
#include "PrefixCursor.h"

using namespace std;

namespace mmt {
    namespace sapt {

        class index_exception : public exception {
        public:
            index_exception(const string &msg) : message(msg) {};

            virtual const char *what() const throw() override {
                return message.c_str();
            }

        private:
            string message;
        };

        struct sample_t {
            domain_t domain;
            vector<wid_t> source;
            vector<wid_t> target;
            alignment_t alignment;
            vector<length_t> offsets;

            string ToString() const{
                ostringstream repr;
                repr << "(" << domain << ")";

                for (auto word = source.begin(); word != source.end(); ++word)
                    repr << " " << *word;
                repr << " |||";
                for (auto word = target.begin(); word != target.end(); ++word)
                    repr << " " << *word;
                repr << " |||";
                for (auto a = alignment.begin(); a != alignment.end(); ++a)
                    repr << " " << a->first << "-" << a->second;
                repr << " ||| offsets:";
                for (auto o = offsets.begin(); o != offsets.end(); ++o)
                    repr << " " << *o;

                return repr.str();
            }
//friend
            friend std::ostream &operator<<(std::ostream &os, const sample_t &sample)
            {
                os << sample.ToString();
                return os;
            }
        };


        class SuffixArray {
        public:
            SuffixArray(const string &path, uint8_t prefixLength,
                        bool prepareForBulkLoad = false) throw(index_exception, storage_exception);

            ~SuffixArray();

            void GetRandomSamples(const vector<wid_t> &phrase, size_t limit, vector<sample_t> &outSamples,
                                  const context_t *context = NULL, bool searchInBackground = true);

            size_t CountOccurrences(bool isSource, const vector<wid_t> &phrase);

            void PutBatch(UpdateBatch &batch) throw(index_exception, storage_exception);

            void ForceCompaction();

            const vector<seqid_t> &GetStreams() const {
                return streams;
            }

        private:
            const uint8_t prefixLength;

            rocksdb::DB *db;
            CorpusStorage *storage;
            vector<seqid_t> streams;

            void AddPrefixesToBatch(bool isSource, domain_t domain, const vector<wid_t> &sentence,
                                    int64_t location, unordered_map<string, PostingList> &outBatch);

            void CollectLocations(PrefixCursor *cursor, const vector<wid_t> &sentence, PostingList &output);

            void CollectLocations(PrefixCursor *cursor, const vector<wid_t> &phrase, size_t offset, size_t length,
                                  PostingList &output);

            void Retrieve(const samplemap_t &locations, vector<sample_t> &outSamples);
        };

    }
}

#endif //SAPT_SUFFIXARRAY_H
