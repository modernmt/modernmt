//
// Created by Davide  Caroselli on 12/05/17.
//

#ifndef MMT_PTDECODER_LEXICALMODEL_H
#define MMT_PTDECODER_LEXICALMODEL_H

#include <string>
#include <mmt/sentence.h>
#include <mmt/vocabulary/Vocabulary.h>

namespace mmt {
    namespace sapt {

        class LexicalModel {
        public:
            static constexpr double kNullProbability = 1e-9;
            static constexpr wid_t kNullWord = 0;

            LexicalModel(const std::string &path);

            inline float GetForwardProbability(wid_t source, wid_t target) const {
                return at(source, target, 0);
            }

            inline float GetBackwardProbability(wid_t source, wid_t target) const {
                return at(source, target, 1);
            }

            inline float GetSourceNullProbability(wid_t source) const {
                return GetForwardProbability(source, kNullWord);
            }

            float GetTargetNullProbability(wid_t target) const {
                return GetForwardProbability(kNullWord, target);
            }

            void Store(const std::string &path);

            static LexicalModel *Import(Vocabulary &vb, const std::string &path);

        private:
            std::vector<std::unordered_map<wid_t, std::pair<float, float>>> model;

            LexicalModel();

            inline float at(wid_t source, wid_t target, size_t index) const {
                if (model.empty() || source >= model.size())
                    return (float) kNullProbability;

                const std::unordered_map<wid_t, std::pair<float, float>> &row = model[source];
                auto ptr = row.find(target);
                return ptr == row.end() ? ((float) kNullProbability) : (index == 0 ? ptr->second.first
                                                                                   : ptr->second.second);
            };
        };

    }
}

#endif //MMT_PTDECODER_LEXICALMODEL_H
