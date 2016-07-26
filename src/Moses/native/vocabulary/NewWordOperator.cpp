//
// Created by Davide Caroselli on 26/07/16.
//

#include "NewWordOperator.h"
#include "Vocabulary.h"

#include <iostream>

using namespace std;

bool NewWordOperator::FullMerge(const rocksdb::Slice &key, const rocksdb::Slice *existing_value,
                                const std::deque<std::string> &operand_list, std::string *new_value,
                                rocksdb::Logger *logger) const {
    if (existing_value == nullptr) {
        uint32_t maxId = 0;

        for (auto i = operand_list.begin(); i != operand_list.end(); ++i) {
            uint32_t temp;
            uint32_t id = Vocabulary::Deserialize(*i, &temp) ? temp : 0;

            if (id > maxId)
                maxId = id;
        }

        uint8_t buffer[4];
        Vocabulary::Serialize(maxId, buffer);
        *new_value = std::string(reinterpret_cast<char const *>(buffer), 4);
    }

    return true;
}

bool NewWordOperator::PartialMerge(const rocksdb::Slice &key, const rocksdb::Slice &left_operand,
                                   const rocksdb::Slice &right_operand, std::string *new_value,
                                   rocksdb::Logger *logger) const {
    uint32_t temp;
    uint32_t id1 = Vocabulary::Deserialize(left_operand, &temp) ? temp : 0;
    uint32_t id2 = Vocabulary::Deserialize(right_operand, &temp) ? temp : 0;

    uint8_t buffer[4];
    Vocabulary::Serialize((id1 > id2 ? id1 : id2), buffer);
    *new_value = std::string(reinterpret_cast<char const *>(buffer), 4);

    return true;
}

bool NewWordOperator::PartialMergeMulti(const rocksdb::Slice &key,
                                        const deque<rocksdb::Slice, allocator<rocksdb::Slice>> &operand_list,
                                        std::string *new_value, rocksdb::Logger *logger) const {

    uint32_t maxId = 0;

    for (auto i = operand_list.begin(); i != operand_list.end(); ++i) {
        uint32_t temp;
        uint32_t id = Vocabulary::Deserialize(*i, &temp) ? temp : 0;

        if (id > maxId)
            maxId = id;
    }

    uint8_t buffer[4];
    Vocabulary::Serialize(maxId, buffer);
    *new_value = std::string(reinterpret_cast<char const *>(buffer), 4);

    return true;
}
