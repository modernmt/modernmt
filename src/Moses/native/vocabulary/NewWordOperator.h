//
// Created by Davide Caroselli on 26/07/16.
//

#ifndef MOSESDECODER_NEWWORDOPERATOR_H
#define MOSESDECODER_NEWWORDOPERATOR_H

#include "IdGenerator.h"
#include <rocksdb/merge_operator.h>
#include <string>

using namespace std;

class NewWordOperator : public rocksdb::MergeOperator {
public:
    virtual bool FullMerge(const rocksdb::Slice &key, const rocksdb::Slice *existing_value,
                           const std::deque<std::string> &operand_list, std::string *new_value,
                           rocksdb::Logger *logger) const override;

    virtual bool
    PartialMerge(const rocksdb::Slice &key, const rocksdb::Slice &left_operand, const rocksdb::Slice &right_operand,
                 std::string *new_value, rocksdb::Logger *logger) const override;

    virtual bool
    PartialMergeMulti(const rocksdb::Slice &key, const deque<rocksdb::Slice, allocator<rocksdb::Slice>> &operand_list,
                      std::string *new_value, rocksdb::Logger *logger) const override;

    virtual const char *Name() const override {
        return "NewWordOperator";
    }
};


#endif //MOSESDECODER_NEWWORDOPERATOR_H
