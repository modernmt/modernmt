//
// Created by Davide  Caroselli on 15/05/17.
//

#ifndef MMT_PBDECODER_VOCABULARY_H
#define MMT_PBDECODER_VOCABULARY_H

#include <mmt/sentence.h>
#include <boost/thread/shared_mutex.hpp>
#include <unordered_set>
#include <fstream>

namespace mmt {

    const wid_t kVocabularyUnknownWord = 0;
    const wid_t kVocabularyWordIdStart = 1000;

    class vocabulary_exception : public std::runtime_error {
    public:
        vocabulary_exception(const std::string &msg) : std::runtime_error(msg) {};
    };

    class Vocabulary {
    public:
        Vocabulary(const std::string &path, bool readonly = false,
                   const std::unordered_set<std::string> *words = nullptr);

        wid_t Lookup(const std::string &word, bool putIfAbsent);

        inline void Lookup(const std::vector<std::string> &line, sentence_t &output, bool putIfAbsent) {
            std::vector<std::vector<std::string>> buffer(1);
            std::vector<sentence_t> out(1);

            buffer[0] = line;
            Lookup(buffer, out, putIfAbsent);
            output = out[0];
        }

        void Lookup(const std::vector<std::vector<std::string>> &buffer, std::vector<sentence_t> &output,
                    bool putIfAbsent);

        bool ReverseLookup(wid_t id, std::string &output);

        inline void ReverseLookup(const sentence_t &sentence, std::vector<std::string> &output) {
            std::vector<sentence_t> buffer(1);
            std::vector<std::vector<std::string>> out(1);

            buffer[0] = sentence;
            ReverseLookup(buffer, out);
            output = out[0];
        }

        void
        ReverseLookup(const std::vector<sentence_t> &buffer, std::vector<std::vector<std::string>> &output);

        ~Vocabulary() {};

    private:
        const bool readonly;
        boost::shared_mutex access;

        FILE *file;
        std::vector<std::string> terms;
        std::unordered_map<std::string, wid_t> vocab;

        inline wid_t Add(const std::string &word) {
            wid_t id = (wid_t) terms.size() + kVocabularyWordIdStart;
            terms.push_back(word);
            vocab[word] = id;
            return id;
        }
    };
}

#endif //MMT_PBDECODER_VOCABULARY_H
