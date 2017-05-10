//
// Created by Davide  Caroselli on 23/08/16.
//

#include "Corpus.h"
#include "Vocabulary.h"
#include <boost/filesystem.hpp>

#define AlignFileExt "align"

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

namespace fs = boost::filesystem;

void Corpus::List(const string &path, const string &outPath,
                  const string &sourceLang, const string &targetLang, vector<Corpus> &list) {
    fs::recursive_directory_iterator endit;

    for (fs::recursive_directory_iterator it(path); it != endit; ++it) {
        fs::path file = fs::absolute(*it);

        if (!fs::is_regular_file(file))
            continue;

        if (file.extension().string() == "." + sourceLang) {
            fs::path sourceFile = file;
            fs::path targetFile = file;
            fs::path alignmentFile = outPath / file.filename();

            targetFile = targetFile.replace_extension(fs::path(targetLang));
            alignmentFile = alignmentFile.replace_extension(fs::path(AlignFileExt));

            if (!fs::is_regular_file(targetFile))
                continue;

            if (fs::is_regular_file(alignmentFile)) //alignment file already present
                continue;

            list.push_back(Corpus(sourceFile.string(), targetFile.string(), alignmentFile.string()));

        }
    }
}

static inline void ParseLine(const string &line, vector<string> &output) {
    output.clear();

    std::stringstream stream(line);
    std::string word;

    while (std::getline(stream, word, ' '))
        output.push_back(word);
}

static inline void ParseLine(const Vocabulary *vocab, const string &line, vector<wid_t> &output) {
    output.clear();

    std::stringstream stream(line);
    std::string word;

    while (std::getline(stream, word, ' '))
        output.push_back(vocab->Get(word));
}

CorpusReader::CorpusReader(const Corpus &corpus, const Vocabulary *vocabulary)
        : drained(false), vocabulary(vocabulary), source(corpus.sourcePath.c_str()), target(corpus.targetPath.c_str()) {

}

bool CorpusReader::Read(vector<string> &outSource, vector<string> &outTarget) {
    if (drained)
        return false;

    string sourceLine, targetLine;
    if (!getline(source, sourceLine) || !getline(target, targetLine)) {
        drained = true;
        return false;
    }

    ParseLine(sourceLine, outSource);
    ParseLine(targetLine, outTarget);

    return true;
}

bool CorpusReader::Read(vector<pair<vector<string>, vector<string>>> &outBuffer, size_t limit) {
    if (drained)
        return false;

    vector<pair<string, string>> batch;
    for (size_t i = 0; i < limit; ++i) {
        string sourceLine, targetLine;
        if (!getline(source, sourceLine) || !getline(target, targetLine)) {
            drained = true;
            break;
        }

        batch.push_back(pair<string, string>(sourceLine, targetLine));
    }

    if (batch.empty())
        return false;

    outBuffer.resize(batch.size());
#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < batch.size(); ++i) {
        ParseLine(batch[i].first, outBuffer[i].first);
        ParseLine(batch[i].second, outBuffer[i].second);
    }

    return true;
}

bool CorpusReader::Read(std::vector<wid_t> &outSource, std::vector<wid_t> &outTarget) {
    if (drained)
        return false;

    string sourceLine, targetLine;
    if (!getline(source, sourceLine) || !getline(target, targetLine)) {
        drained = true;
        return false;
    }

    ParseLine(vocabulary, sourceLine, outSource);
    ParseLine(vocabulary, targetLine, outTarget);

    return true;
}

bool CorpusReader::Read(std::vector<std::pair<std::vector<wid_t>, std::vector<wid_t>>> &outBuffer, size_t limit) {
    if (drained)
        return false;

    vector<pair<string, string>> batch;
    for (size_t i = 0; i < limit; ++i) {
        string sourceLine, targetLine;
        if (!getline(source, sourceLine) || !getline(target, targetLine)) {
            drained = true;
            break;
        }

        batch.push_back(pair<string, string>(sourceLine, targetLine));
    }

    if (batch.empty())
        return false;

    outBuffer.resize(batch.size());
#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < batch.size(); ++i) {
        ParseLine(vocabulary, batch[i].first, outBuffer[i].first);
        ParseLine(vocabulary, batch[i].second, outBuffer[i].second);
    }

    return true;
}
