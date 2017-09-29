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
            memory_t memory;

            location_t(int64_t pointer = -1, length_t offset = 0, memory_t memory = 0)
                    : pointer(pointer), offset(offset), memory(memory) {}
        };

        class PostingList {
        public:

            static const size_t kEntrySize = sizeof(int64_t) + sizeof(length_t);

            PostingList();

            void Append(memory_t memory, const string &value);

            void Append(memory_t memory, int64_t location, length_t offset);

            void Retain(const PostingList *successors, size_t start);

            void GetLocations(vector<location_t> &output, size_t limit = 0, unsigned int seed = 0);

            bool empty() const;

            size_t size() const;

            string Serialize() const;

            static void Deserialize(const char *data, size_t length, vector<location_t> &output);

        private:
            size_t entryCount;
            map<memory_t, vector<char>> datamap;

            void GetLocationMap(memory_t memory, unordered_map<int64_t, unordered_set<length_t>> &output) const;

            inline void Get(const vector<char> &chunk, size_t index, int64_t *outLocation, length_t *outOffset);
        };

    }
}


#endif //SAPT_POSTINGLIST_H
