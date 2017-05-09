//
// Created by Davide  Caroselli on 23/08/16.
//

#include "Corpus.h"
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

CorpusReader::CorpusReader(const Corpus &corpus) : drained(false), source(corpus.sourcePath.c_str()),
                                                   target(corpus.targetPath.c_str()) {
}

bool CorpusReader::Read(vector<wid_t> &outSource, vector<wid_t> &outTarget) {
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

bool CorpusReader::Read(vector<pair<vector<wid_t>, vector<wid_t>>> &outBuffer, size_t limit) {
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
