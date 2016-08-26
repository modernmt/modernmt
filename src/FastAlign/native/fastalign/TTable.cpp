// Copyright 2013 by Chris Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

#include "TTable.h"
#include <string>
#include <fstream>

using namespace std;

void TTable::NormalizeVB(const double alpha) {
    ttable.swap(counts);
#pragma omp parallel for schedule(dynamic)
    for (unsigned i = 0; i < ttable.size(); ++i) {
        double tot = 0;
        Word2Double& cpd = ttable[i];
        for (Word2Double::iterator it = cpd.begin(); it != cpd.end(); ++it)
            tot += it->second + alpha;
        if (!tot) tot = 1;
        const double digamma_tot = Md::digamma(tot);
        for (Word2Double::iterator it = cpd.begin(); it != cpd.end(); ++it)
            it->second = exp(Md::digamma(it->second + alpha) - digamma_tot);
    }
    ClearCounts();
    probs_initialized_ = true;
}

void TTable::Normalize() {
    ttable.swap(counts);
#pragma omp parallel for schedule(dynamic)
    for (unsigned i = 0; i < ttable.size(); ++i) {
        double tot = 0;
        Word2Double& cpd = ttable[i];
        for (Word2Double::iterator it = cpd.begin(); it != cpd.end(); ++it)
            tot += it->second;
        if (!tot) tot = 1;
        for (Word2Double::iterator it = cpd.begin(); it != cpd.end(); ++it)
            it->second = it->second / tot;
    }
    ClearCounts();
    probs_initialized_ = true;
}

void TTable::Freeze() {
    // duplicate all values in counts into ttable
    // later updates to both are semi-threadsafe
    assert(!frozen_);
    if (!frozen_) {
#pragma omp critical(update_ttable)  // aggiunta Lorenzo
        {
            ttable.resize(counts.size());
            for (unsigned i = 0; i < counts.size(); ++i) {
                ttable[i] = counts[i];
            }
        }
    }
    frozen_ = true;
}

void TTable::SaveToBinFile(std::ofstream &out, double DiagonalTension, double MeanSourceLenMultiplier) const {

    out.write((char*) &DiagonalTension, sizeof (double));
    out.write((char*) &MeanSourceLenMultiplier, sizeof (double));

    for (unsigned i = 0; i < ttable.size(); ++i) {
        const Word2Double& cpd = ttable[i];
        for (Word2Double::const_iterator it = cpd.begin(); it != cpd.end(); ++it) {
            out.write((char*) &i, sizeof (unsigned));
            out.write((char*) &it->first, sizeof (unsigned));
            out.write((char*) &it->second, sizeof (double));
        }
    }
}

void TTable::LoadFromBinFile(std::ifstream &in, double& DiagonalTension, double& MeanSourceLenMultiplier) {
    // TODO
//    cerr << "Reading Lexical Translation Table" << endl;
//
//    in.read(reinterpret_cast<char*> (&DiagonalTension), sizeof (double));
//    in.read(reinterpret_cast<char*> (&MeanSourceLenMultiplier), sizeof (double));
//    unsigned ide, idf;
//    double prob;
//
//    ttable.resize(d.size() + 1);
//
//    streampos current, fsize;
//    current = in.tellg(); // Get current position
//    in.seekg(0, in.end); // Go to the end of the file
//    fsize = in.tellg(); // Get the position. No we can calculate the table size
//    in.seekg(current); // Go back to your original position (where you need to start reading the file)
//
//    assert (fsize > current);
//    size_t modelByteSize = (size_t) (fsize - current); //byte size of the model
//    assert (modelByteSize < SIZE_MAX);
//    assert (modelByteSize % (2 * sizeof (unsigned) + sizeof (double) ) == 0);
//
//    vector<char> bytes(modelByteSize);
//    in.read(&bytes[0], modelByteSize); // Read everything into memory
//    size_t offset = 0;
//    while (offset < modelByteSize) {
//        ide = *reinterpret_cast<const unsigned*> (&bytes[offset]);
//        idf = *reinterpret_cast<const unsigned*> (&bytes[offset + sizeof (unsigned)]);
//        prob = *reinterpret_cast<double*> (&bytes[offset + 2 * sizeof (unsigned)]);
//        offset += 2 * sizeof (unsigned) + sizeof (double);
//        ttable[ide][idf] = prob;
//    }
//
//    probs_initialized_ = true;
}

void TTable::ClearCounts() {
#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < counts.size(); ++i) {
        for (Word2Double::iterator cnt = counts[i].begin(); cnt != counts[i].end(); ++cnt) {
            cnt->second = 0.0;
        }
    }
}

void TTable::Prune(double threshold) {
    for (unsigned i = 0; i < ttable.size(); ++i) {
        Word2Double& cpd = ttable[i];
        for (Word2Double::const_iterator it = cpd.cbegin(); it != cpd.cend(); /* no increment */) {
            if (it->second < threshold) {
                cpd.erase(it++);
            } else {
                ++it;
            }
        }
    }
}