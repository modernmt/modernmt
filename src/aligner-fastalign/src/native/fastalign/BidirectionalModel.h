//
// Created by Davide  Caroselli on 08/05/17.
//

#ifndef MMT_FASTALIGN_BIDIRECTIONALMODEL_H
#define MMT_FASTALIGN_BIDIRECTIONALMODEL_H

using namespace std;

#include <memory>
#include <vector>
#include <string>

#include "Model.h"
#include "Vocabulary.h"

namespace mmt {
    namespace fastalign {

        typedef std::vector<std::unordered_map<word_t, std::pair<float, float>>> bitable_t;

        class BidirectionalModel : public Model {
        public:
            BidirectionalModel(std::shared_ptr<bitable_t> table, bool forward, bool use_null,
                               bool favor_diagonal, double prob_align_null, double diagonal_tension);

            inline double GetProbability(word_t source, word_t target) override {
                if (is_reverse)
                    std::swap(source, target);

                if (table->empty())
                    return kNullProbability;
                if (source >= table->size())
                    return kNullProbability;

                std::unordered_map<word_t, std::pair<float, float>> &row = table->at(source);
                auto ptr = row.find(target);
                return ptr == row.end() ? kNullProbability : (is_reverse ? ptr->second.second : ptr->second.first);
            }

            inline void IncrementProbability(word_t source, word_t target, double amount) override {
                // no-op
            }

            void ExportLexicalModel(const string &filename, const Vocabulary *vb);

            static void Store(const BidirectionalModel *forward, const BidirectionalModel *backward,
                              const string &filename);

            static void Store(const string &fwd_path, const string &bwd_path,
                                                  const string &out_path);

            static void Open(const string &filename, Model **outForward, Model **outBackward);

        private:
            const std::shared_ptr<bitable_t> table;
        };
    }
}


#endif //MMT_FASTALIGN_BIDIRECTIONALMODEL_H
