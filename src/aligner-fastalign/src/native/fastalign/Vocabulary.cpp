//
// Created by Davide  Caroselli on 09/05/17.
//

#include "Vocabulary.h"
#include <iostream>
#include <unordered_map>
#include <algorithm>
#include <math.h>
#include "ioutils.h"

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

inline score_t SmoothInverseDocumentFrequency(size_t n_docs, size_t doc_freq) {
    return static_cast<score_t>(log(((double) n_docs) / (1. + doc_freq)));
}

const Vocabulary *Vocabulary::BuildFromCorpora(const std::vector<Corpus> &corpora, const std::string &filename,
                                               size_t maxLineLength, bool case_sensitive, double threshold) {
    boost::locale::generator gen;
    std::locale locale = gen("C.UTF-8");

    // For model efficiency all source words must have the lowest id possible
    unordered_map<string, size_t> src_terms;
    unordered_map<string, size_t> tgt_terms;
    unordered_map<string, size_t> src_doc_term_freq;
    unordered_map<string, size_t> tgt_doc_term_freq;

    vector<string> src, trg;
    unordered_set<string> src_doc_terms, tgt_doc_terms;
    size_t n_docs = 0;

    for (auto corpus = corpora.begin(); corpus != corpora.end(); ++corpus) {
        CorpusReader reader(*corpus, nullptr, maxLineLength, true);

        while (reader.Read(src, trg)) {
            src_doc_terms.clear();
            tgt_doc_terms.clear();

            for (auto w = src.begin(); w != src.end(); ++w) {
                string src_term = case_sensitive ? *w : boost::locale::to_lower(*w, locale);
                src_terms[src_term] += 1;
                src_doc_terms.insert(src_term);
            }

            for (auto w = trg.begin(); w != trg.end(); ++w) {
                string tgt_term = case_sensitive ? *w : boost::locale::to_lower(*w, locale);
                tgt_terms[tgt_term] += 1;
                tgt_doc_terms.insert(tgt_term);
            }

            n_docs += 1;
            for (auto w = src_doc_terms.begin(); w != src_doc_terms.end(); ++w)
                src_doc_term_freq[*w] += 1;
            for (auto w = tgt_doc_terms.begin(); w != tgt_doc_terms.end(); ++w)
                tgt_doc_term_freq[*w] += 1;
        }
    }

    if (threshold > 0) {
        PruneTerms(src_terms, threshold);
        PruneTerms(tgt_terms, threshold);
    }

    // Create source and target terms array

    vector<pair<string, size_t>> src_terms_array, tgt_terms_array;
    src_terms_array.reserve(src_terms.size());
    tgt_terms_array.reserve(tgt_terms.size());

    for (auto src_term = src_terms.begin(); src_term != src_terms.end(); ++src_term) {
        src_terms_array.emplace_back(src_term->first, src_term->second);
    }

    for (auto tgt_term = tgt_terms.begin(); tgt_term != tgt_terms.end(); ++tgt_term) {
        auto src_term = src_terms.find(tgt_term->first);
        if (src_term == src_terms.end())
            tgt_terms_array.emplace_back(tgt_term->first, tgt_term->second);
    }

    std::sort(src_terms_array.begin(), src_terms_array.end(), __terms_compare);
    std::sort(tgt_terms_array.begin(), tgt_terms_array.end(), __terms_compare);

    // Writing output model

    ofstream out(filename);

    ostringstream header;
    header << "size=" << (src_terms_array.size() + tgt_terms_array.size()) << ' '
           << "case_sensitive=" << (case_sensitive ? '1' : '0');

    string header_str = header.str();
    io_write(out, header_str);

    for (auto src_term = src_terms_array.begin(); src_term != src_terms_array.end(); ++src_term) {
        size_t src_doc_freq = src_doc_term_freq[src_term->first];
        size_t tgt_doc_freq = 0;

        auto tgt_term = tgt_doc_term_freq.find(src_term->first);
        if (tgt_term != tgt_doc_term_freq.end())
            tgt_doc_freq = tgt_term->second;

        io_write(out, SmoothInverseDocumentFrequency(n_docs, src_doc_freq));
        io_write(out, SmoothInverseDocumentFrequency(n_docs, tgt_doc_freq));
        io_write(out, src_term->first);
    }

    for (auto tgt_term = tgt_terms_array.begin(); tgt_term != tgt_terms_array.end(); ++tgt_term) {
        size_t src_doc_freq = 0;
        size_t tgt_doc_freq = tgt_doc_term_freq[tgt_term->first];

        io_write(out, SmoothInverseDocumentFrequency(n_docs, src_doc_freq));
        io_write(out, SmoothInverseDocumentFrequency(n_docs, tgt_doc_freq));
        io_write(out, tgt_term->first);
    }

    out.close();

    return new Vocabulary(filename);
}

Vocabulary::Vocabulary(const std::string &filename) {
    boost::locale::generator gen;
    locale = gen("C.UTF-8");

    ifstream in(filename);

    string header;
    io_read(in, header);

    size_t size;
    ParseHeader(header, &size, &case_sensitive);

    probs.resize(size + 2);
    vocab.reserve(size + 2);

    for (word_t id = 2; id < size + 2; ++id) {
        probs[id].first = io_read<score_t>(in);
        probs[id].second = io_read<score_t>(in);

        string word;
        io_read(in, word);
        vocab[word] = id;
    }
}
