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

        struct location_t {
            int64_t pointer;
            length_t offset;
            domain_t domain;

            location_t(int64_t pointer = -1, length_t offset = 0, domain_t domain = 0)
                    : pointer(pointer), offset(offset), domain(domain) {}
        };

        class PostingList {
        public:

            static const size_t kEntrySize = sizeof(int64_t) + sizeof(length_t);

            PostingList();

            void Append(domain_t domain, const string &value);

            void Append(domain_t domain, int64_t location, length_t offset);

            void Retain(const PostingList *successors, size_t start);

            void GetLocations(vector<location_t> &output, size_t limit = 0, unsigned int seed = 0);

            bool empty() const;

            size_t size() const;

            string Serialize() const;

            static void Deserialize(const char *data, size_t length, vector<location_t> &output);

        private:
            size_t entryCount;
            map<domain_t, vector<char>> datamap;

            void GetLocationMap(domain_t domain, unordered_map<int64_t, unordered_set<length_t>> &output) const;

            inline void Get(const vector<char> &chunk, size_t index, int64_t *outLocation, length_t *outOffset);
        };

    }
}


#endif //SAPT_POSTINGLIST_H
