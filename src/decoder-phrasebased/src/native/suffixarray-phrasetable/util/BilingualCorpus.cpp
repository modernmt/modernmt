//
// Created by Davide  Caroselli on 29/09/16.
//

#include <sstream>
#include "BilingualCorpus.h"
#include <boost/filesystem.hpp>

#define AlignFileExt "align"

using namespace mmt;
using namespace mmt::sapt;

namespace fs = boost::filesystem;

BilingualCorpus::BilingualCorpus(memory_t memory, const string &sourceFile, const string &targetFile,
                                 const string &alignmentFile)
        : memory(memory), source(sourceFile), target(targetFile), alignment(alignmentFile) {
}

static inline void ParseSentenceLine(Vocabulary &vb, const string &line, sentence_t &output) {
    output.clear();

    stringstream stream(line);
    string word;

    while (stream >> word) {
        output.push_back(vb.Lookup(word, false));
    }

}

static inline void ParseAlignmentLine(const string &line, alignment_t &output) {
    output.clear();

    stringstream stream(line);
    string pair;

    while (stream >> pair) {
        char dash;
        length_t i, j;

        stringstream pair_stream(pair);

        pair_stream >> i;
        pair_stream >> dash;
        pair_stream >> j;

        output.push_back(make_pair(i, j));
    }
}

void BilingualCorpus::List(const string &path, const string &sourceLang, const string &targetLang,
                           vector<BilingualCorpus> &list) {
    fs::recursive_directory_iterator endit;

    for (fs::recursive_directory_iterator it(path); it != endit; ++it) {
        fs::path file = fs::absolute(*it);

        if (!fs::is_regular_file(file))
            continue;

        if (file.extension().string() == "." + sourceLang) {
            fs::path sourceFile = file;
            fs::path targetFile = file;
            fs::path alignmentFile = file;

            targetFile = targetFile.replace_extension(fs::path(targetLang));
            alignmentFile = alignmentFile.replace_extension(fs::path(AlignFileExt));

            if (!fs::is_regular_file(targetFile))
                continue;

            if (!fs::is_regular_file(alignmentFile))
                continue;

            memory_t memory = (memory_t) stoi(file.stem().string());

            list.push_back(BilingualCorpus(memory, sourceFile.string(), targetFile.string(), alignmentFile.string()));
        }
    }
}

CorpusReader::CorpusReader(Vocabulary &vocabulary, const BilingualCorpus &corpus)
        : vb(vocabulary), drained(false), sourceStream(corpus.source), targetStream(corpus.target),
          alignmentStream(corpus.alignment) {
}

bool CorpusReader::Read(vector<wid_t> &outSource, vector<wid_t> &outTarget, alignment_t &outAlignment) {
    if (drained)
        return false;

    string sourceLine, targetLine, alignmentLine;
    if (!getline(sourceStream, sourceLine) || !getline(targetStream, targetLine) ||
        !getline(alignmentStream, alignmentLine)) {
        drained = true;
        return false;
    }

    ParseSentenceLine(vb, sourceLine, outSource);
    ParseSentenceLine(vb, targetLine, outTarget);
    ParseAlignmentLine(alignmentLine, outAlignment);

    return true;
}