//
// Created by Davide  Caroselli on 30/09/16.
//

#ifndef SAPT_POSTINGLIST_H
#define SAPT_POSTINGLIST_H

#include <string>
#include <mmt/sentence.h>
#include <unordered_set>
#include <map>

using namespace std;

namespace mmt {
    namespace sapt {

        typedef unordered_map<domain_t, unordered_map<int64_t, vector<length_t>>> samplemap_t;

        class PostingList {
        public:

            PostingList();

            PostingList(const vector<wid_t> &phrase) :
                    PostingList(phrase, 0, phrase.size()) {};

            PostingList(const vector<wid_t> &sentence, size_t offset, size_t size);

            void Append(domain_t domain, const string &value);

            void Append(domain_t domain, int64_t location, length_t offset);

            void Join(const PostingList &other);

            void Retain(const PostingList &successors, length_t start);

            void GetSamples(samplemap_t &output, size_t limit = 0);

            bool empty() const;

            size_t size() const;

            string Serialize() const;

        private:
            const unsigned int phraseHash;
            size_t entryCount;
            map<domain_t, vector<char>> datamap;

            void GetLocationMap(unordered_map<int64_t, unordered_set<length_t>> &output) const;

            inline void Get(const vector<char> &chunk, size_t index, int64_t *outLocation, length_t *outOffset);
        };

    }
}


#endif //SAPT_POSTINGLIST_H
