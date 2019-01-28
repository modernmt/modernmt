//
// Created by Davide  Caroselli on 09/05/17.
//

#include "Vocabulary.h"
#include <iostream>
#include <unordered_map>
#include <algorithm>

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

void ParseHeader(const string &header, size_t *outSize, bool *outCaseSensitive) {
    vector<string> properties;

    istringstream iss(header);
    copy(istream_iterator<string>(iss), istream_iterator<string>(), back_inserter(properties));

    for (auto property = properties.begin(); property != properties.end(); ++property) {
        istringstream property_ss(*property);

        string key;
        if (getline(property_ss, key, '=')) {
            string value;
            if (getline(property_ss, value)) {
                if ("size" == key)
                    *outSize = (size_t) stoi(value);
                else if ("case_sensitive" == key)
                    *outCaseSensitive = (value[0] == '1');
                else
                    throw runtime_error("invalid header key: " + key);
            }
        }
    }
}

bool __terms_compare(const pair<string, size_t> &a, const pair<string, size_t> &b) {
    return b.second < a.second;
}

inline void PruneTerms(unordered_map<string, size_t> &terms, double threshold) {
    vector<pair<string, size_t>> entries;
    entries.resize(terms.size());

    size_t total = 0;
    size_t i = 0;
    for (auto entry = terms.begin(); entry != terms.end(); ++entry) {
        entries[i].first = entry->first;
        entries[i].second = entry->second;

        total += entry->second;

        i++;
    }

    std::sort(entries.begin(), entries.end(), __terms_compare);

    double counter = 0;
    size_t min_size = 0;
    for (auto entry = entries.begin(); entry != entries.end(); ++entry) {
        counter += entry->second;

        if (counter / total >= threshold) {
            min_size = entry->second;
            break;
        }
    }

    if (min_size > 1) {
        for (auto entry = terms.begin(); entry != terms.end(); /* no increment */) {
            if (entry->second < min_size)
                entry = terms.erase(entry);
            else
                ++entry;
        }
    }
}

const Vocabulary *Vocabulary::FromCorpora(const vector<Corpus> &corpora,
                                          size_t maxLineLength, bool case_sensitive, double threshold) {
    boost::locale::generator gen;
    std::locale locale = gen("C.UTF-8");

    // For model efficiency all source words must have the lowest id possible
    unordered_map<string, size_t> src_terms;
    unordered_map<string, size_t> tgt_terms;

    size_t src_corpora_size = 0;
    size_t tgt_corpora_size = 0;
    vector<string> src, trg;

    for (auto corpus = corpora.begin(); corpus != corpora.end(); ++corpus) {
        CorpusReader reader(*corpus, nullptr, maxLineLength, true);

        while (reader.Read(src, trg)) {
            for (auto w = src.begin(); w != src.end(); ++w)
                src_terms[case_sensitive ? *w : boost::locale::to_lower(*w, locale)] += 1;

            for (auto w = trg.begin(); w != trg.end(); ++w)
                tgt_terms[case_sensitive ? *w : boost::locale::to_lower(*w, locale)] += 1;

            src_corpora_size += src.size();
            tgt_corpora_size += trg.size();
        }
    }

    if (threshold > 0) {
        PruneTerms(src_terms, threshold);
        PruneTerms(tgt_terms, threshold);
    }

    // Creating result Vocabulary

    auto result = new Vocabulary(case_sensitive);
    word_t id = 2; // 0 = kNullWord, 1 = kUnknownWord

    for (auto src_term = src_terms.begin(); src_term != src_terms.end(); ++src_term) {
        double src_freq = src_term->second;
        double tgt_freq = 0;

        auto tgt_term = tgt_terms.find(src_term->first);
        if (tgt_term != tgt_terms.end()) {
            tgt_freq = tgt_term->second;
            tgt_terms.erase(tgt_term->first);  // we don't want to register it twice with the next loop
        }

        result->vocab[src_term->first] = id;

        result->probs.resize(id + 1);
        result->probs[id].first = static_cast<score_t>(src_freq / src_corpora_size);
        result->probs[id].second = static_cast<score_t>(tgt_freq / tgt_corpora_size);

        id++;
    }

    for (auto tgt_term = tgt_terms.begin(); tgt_term != tgt_terms.end(); ++tgt_term) {
        // "double src_freq" we cannot have a match here because we have erased shared terms at the previous loop
        double tgt_freq = tgt_term->second;

        result->vocab[tgt_term->first] = id;

        result->probs.resize(id + 1);
        result->probs[id].first = 0;
        result->probs[id].second = static_cast<score_t>(tgt_freq / tgt_corpora_size);

        id++;
    }

    return result;
}

Vocabulary::Vocabulary(const std::string &filename) {
    boost::locale::generator gen;
    locale = gen("C.UTF-8");

    ifstream input(filename);
    string line;

    size_t size;
    getline(input, line);  // header
    ParseHeader(line, &size, &case_sensitive);

    probs.resize(size);
    vocab.reserve(size);

    while (getline(input, line)) {
        istringstream line_ss(line);

        string word;
        word_t id;
        score_t src_prob;
        score_t tgt_prob;

        line_ss >> word;
        line_ss >> id;
        line_ss >> src_prob;
        line_ss >> tgt_prob;

        probs[id].first = src_prob;
        probs[id].second = tgt_prob;
        vocab[word] = id;
    }
}

void Vocabulary::Store(const std::string &filename) const {
    ofstream output(filename);

    output << "size=" << Size() << ' '
           << "case_sensitive=" << (case_sensitive ? '1' : '0')
           << '\n';

    for (auto term = vocab.begin(); term != vocab.end(); ++term) {
        const pair<score_t, score_t> &prob = probs[term->second];
        output << term->first << ' '
               << term->second << ' '
               << prob.first << ' '
               << prob.second
               << '\n';
    }
}