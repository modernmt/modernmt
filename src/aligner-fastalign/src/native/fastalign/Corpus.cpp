//
// Created by Davide  Caroselli on 23/08/16.
//


#include <iostream>

#include "Corpus.h"
#include "Vocabulary.h"
#include <boost/filesystem.hpp>

#define AlignFileExt "align"
#define ScoreFileExt "score"


using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

namespace fs = boost::filesystem;

static inline bool length_limit(size_t lenLimit, wordvec_t &outSource, wordvec_t &outTarget){
    if ( outSource.size() > 0 && outSource.size() <= lenLimit && outTarget.size() > 0 && outTarget.size() <= lenLimit )
         return true;
    else
        return false;
}
static inline bool length_limit(size_t lenLimit, sentence_t &outSource, sentence_t &outTarget){
    if ( outSource.size() > 0 && outSource.size() <= lenLimit && outTarget.size() > 0 && outTarget.size() <= lenLimit )
         return true;
    else
        return false;
}


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
            fs::path scoreFile = outPath / file.filename();

            targetFile = targetFile.replace_extension(fs::path(targetLang));
            alignmentFile = alignmentFile.replace_extension(fs::path(AlignFileExt));
            scoreFile = scoreFile.replace_extension(fs::path(ScoreFileExt));

            if (!fs::is_regular_file(targetFile))
                continue;

            if (fs::is_regular_file(alignmentFile)) //alignment file already present
                continue;

            list.push_back(Corpus(sourceFile.string(), targetFile.string(), alignmentFile.string(), scoreFile.string()));
        }
    }
}

static inline void ParseLine(const string &line, sentence_t &output) {
    output.clear();

    std::stringstream stream(line);
    std::string word;

    while (std::getline(stream, word, ' '))
        output.push_back(word);
}

static inline void ParseLine(const Vocabulary *vocab, const string &line, wordvec_t &output) {
    output.clear();

    std::stringstream stream(line);
    std::string word;

    while (std::getline(stream, word, ' '))
        output.push_back(vocab->Get(word));
}

CorpusReader::CorpusReader(const Corpus &corpus, const Vocabulary *vocabulary, const size_t maxL)
        : drained(false), vocabulary(vocabulary), source(corpus.sourcePath.c_str()), target(corpus.targetPath.c_str()), maxLength(maxL) {

}

bool CorpusReader::Read(sentence_t &outSource, sentence_t &outTarget) {
    if (drained)
        return false;

    string sourceLine, targetLine;
    while (true){
        if (!getline(source, sourceLine) || !getline(target, targetLine)) {
            drained = true;
            return false;
        }

        ParseLine(sourceLine, outSource);
        ParseLine(targetLine, outTarget);

        if (length_limit(maxLength, outSource, outTarget)) return true;
    }
}

bool CorpusReader::Read(vector<pair<sentence_t, sentence_t>> &outBuffer, size_t limit) {
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

//    outBuffer.resize(batch.size());
    vector<pair<sentence_t, sentence_t>> outBufferTmp(batch.size());
#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < batch.size(); ++i) {
        ParseLine(batch[i].first, outBufferTmp[i].first);
        ParseLine(batch[i].second, outBufferTmp[i].second);
    }

    outBuffer.resize(batch.size());
    size_t j = 0;
    for (size_t i = 0; i < outBufferTmp.size(); ++i) {

        if (length_limit(maxLength, outBufferTmp[i].first, outBufferTmp[i].second)){
            outBuffer[j].first = outBufferTmp[i].first;
            outBuffer[j].second = outBufferTmp[i].second;
            ++j;
        }
    }
    outBuffer.resize(j);

    if (outBuffer.empty())
        return false;

    return true;
}

bool CorpusReader::Read(wordvec_t &outSource, wordvec_t &outTarget) {
    if (drained)
        return false;

    string sourceLine, targetLine;
    while (true){
        if (!getline(source, sourceLine) || !getline(target, targetLine)) {
            drained = true;
            return false;
        }

        ParseLine(vocabulary, sourceLine, outSource);
        ParseLine(vocabulary, targetLine, outTarget);

        if (length_limit(maxLength, outSource, outTarget)) return true;
    }
}

bool CorpusReader::Read(std::vector<std::pair<wordvec_t, wordvec_t>> &outBuffer, size_t limit) {
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

    std::vector<std::pair<wordvec_t, wordvec_t>> outBufferTmp(batch.size());
//    outBuffer.resize(batch.size());
#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < batch.size(); ++i) {
        ParseLine(vocabulary, batch[i].first, outBufferTmp[i].first);
        ParseLine(vocabulary, batch[i].second, outBufferTmp[i].second);
    }

    outBuffer.resize(batch.size());
    size_t j = 0;
    for (size_t i = 0; i < outBufferTmp.size(); ++i) {
        if (length_limit(maxLength, outBufferTmp[i].first, outBufferTmp[i].second)){
            outBuffer[j].first = outBufferTmp[i].first;
            outBuffer[j].second = outBufferTmp[i].second;
            ++j;
        }
    }
    outBuffer.resize(j);

    if (outBuffer.empty())
        return false;

    return true;
}
