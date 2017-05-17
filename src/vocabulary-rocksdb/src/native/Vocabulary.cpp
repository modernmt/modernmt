//
// Created by Davide  Caroselli on 15/05/17.
//

#include <mutex>
#include <unistd.h>
#include "Vocabulary.h"

using namespace std;
using namespace mmt;

static inline void file_flush(FILE *file) {
    fflush(file);
    fsync(fileno(file));
}

static inline void file_println(FILE *file, const string &string, bool flush = false) {
    size_t size = string.size();
    if (fwrite(string.c_str(), sizeof(char), size, file) != size)
        throw vocabulary_exception("Failed to write to storage");

    if (fwrite("\n", sizeof(char), 1, file) != 1)
        throw vocabulary_exception("Failed to write to storage");

    if (flush)
        file_flush(file);
}

Vocabulary::Vocabulary(const string &path, bool readonly, const unordered_set<string> *words)
        : readonly(readonly) {
    // Load from storage;
    ifstream input(path);
    for (string term; getline(input, term);)
        Add(term);

    // Create reader
    const char *c_path = path.c_str();

    file = fopen(c_path, "a");
    if (!file)
        throw vocabulary_exception("File not found: " + path);

    // Add input words
    if (words) {
        for (auto word = words->begin(); word != words->end(); ++word) {
            Add(*word);
            file_println(file, *word, false);
        }

        file_flush(file);
    }
}

wid_t Vocabulary::Lookup(const string &word, bool putIfAbsent) {
    if (readonly) {
        auto id = vocab.find(word);
        return id == vocab.end() ? kVocabularyUnknownWord : id->second;
    } else if (!putIfAbsent) {
        boost::shared_lock<boost::shared_mutex> lock(access);

        auto id = vocab.find(word);
        return id == vocab.end() ? kVocabularyUnknownWord : id->second;
    } else {
        wid_t id;

        boost::upgrade_lock<boost::shared_mutex> lock(access);

        auto it = vocab.find(word);
        if (it == vocab.end()) {
            boost::upgrade_to_unique_lock<boost::shared_mutex> uniqueLock(lock);

            it = vocab.find(word);
            if (it == vocab.end()) {
                id = Add(word);
                file_println(file, word, true);
            } else {
                id = it->second;
            }
        } else {
            id = it->second;
        }

        return id;
    }
}

void Vocabulary::Lookup(const vector<vector<string>> &buffer, vector<sentence_t> &output, bool putIfAbsent) {
    output.resize(buffer.size());
    for (size_t i = 0; i < buffer.size(); ++i)
        output[i].resize(buffer[i].size());

    if (readonly) {
        for (size_t el = 0; el < buffer.size(); ++el) {
            const vector<string> &in = buffer[el];
            sentence_t &out = output[el];

            for (size_t i = 0; i < in.size(); ++i) {
                auto id = vocab.find(in[i]);
                out[i] = id == vocab.end() ? kVocabularyUnknownWord : id->second;
            }
        }
    } else if (!putIfAbsent) {
        boost::shared_lock<boost::shared_mutex> lock(access);

        for (size_t el = 0; el < buffer.size(); ++el) {
            const vector<string> &in = buffer[el];
            sentence_t &out = output[el];

            for (size_t i = 0; i < in.size(); ++i) {
                auto id = vocab.find(in[i]);
                out[i] = id == vocab.end() ? kVocabularyUnknownWord : id->second;
            }
        }
    } else {
        bool needInsert = false;

        boost::upgrade_lock<boost::shared_mutex> lock(access);

        for (size_t el = 0; el < buffer.size(); ++el) {
            const vector<string> &in = buffer[el];
            sentence_t &out = output[el];

            for (size_t i = 0; i < in.size(); ++i) {
                auto id = vocab.find(in[i]);
                out[i] = id == vocab.end() ? kVocabularyUnknownWord : id->second;

                if (out[i] == kVocabularyUnknownWord)
                    needInsert = true;
            }
        }

        if (needInsert) {
            boost::upgrade_to_unique_lock<boost::shared_mutex> uniqueLock(lock);

            for (size_t el = 0; el < buffer.size(); ++el) {
                const vector<string> &in = buffer[el];
                sentence_t &out = output[el];

                for (size_t i = 0; i < in.size(); ++i) {
                    if (out[i] != kVocabularyUnknownWord)
                        continue;

                    wid_t id;

                    auto it = vocab.find(in[i]);
                    if (it == vocab.end()) {
                        id = Add(in[i]);
                        file_println(file, in[i], false);
                    } else {
                        id = it->second;
                    }

                    out[i] = id;
                }
            }

            file_flush(file);
        }
    }
}

bool Vocabulary::ReverseLookup(wid_t id, string &output) {
    id -= kVocabularyWordIdStart;

    boost::shared_lock<boost::shared_mutex> lock(access);
    if (id < terms.size()) {
        output = terms[id];
        return true;
    } else {
        return false;
    }
}

void Vocabulary::ReverseLookup(const vector<sentence_t> &buffer, vector<vector<string>> &output) {
    output.resize(buffer.size());
    for (size_t i = 0; i < buffer.size(); ++i)
        output[i].resize(buffer[i].size());

    boost::shared_lock<boost::shared_mutex> lock(access);

    for (size_t el = 0; el < buffer.size(); ++el) {
        const sentence_t &in = buffer[el];
        vector<string> &out = output[el];

        for (size_t i = 0; i < in.size(); ++i) {
            wid_t id = in[i] - kVocabularyWordIdStart;
            out[i] = id < terms.size() ? terms[id] : "";
        }
    }
}
