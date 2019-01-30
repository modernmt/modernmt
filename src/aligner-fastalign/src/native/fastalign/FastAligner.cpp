//
// Created by Davide Caroselli on 04/09/16.
//

#include <symal/SymAlignment.h>
#include "FastAligner.h"
#include <thread>
#include <boost/filesystem.hpp>
#include "BidirectionalModel.h"

#ifdef _OPENMP
#include <omp.h>
#endif

namespace fs = boost::filesystem;

using namespace std;
using namespace mmt;
using namespace mmt::fastalign;

FastAligner::FastAligner(const string &path, int threads) {
    fs::path model_path = fs::absolute(fs::path(path));
    if (!fs::is_regular(model_path))
        throw invalid_argument("file not found: " + model_path.string());

    ifstream in(model_path.string(), ios::binary | ios::in);
    vocabulary = Vocabulary(in);
    BidirectionalModel::Open(in, &forwardModel, &backwardModel);
    in.close();

    this->threads = threads > 0 ? threads : (int) thread::hardware_concurrency();
#ifdef _OPENMP
    omp_set_dynamic(0);
    omp_set_num_threads(this->threads);
#endif
}

FastAligner::~FastAligner() {
    delete forwardModel;
    delete backwardModel;
}

alignment_t FastAligner::GetAlignment(const sentence_t &_source, const sentence_t &_target,
                                      Symmetrization symmetrization) {
    wordvec_t source, target;
    vocabulary.Encode(_source, source);
    vocabulary.Encode(_target, target);

    return GetAlignment(source, target, symmetrization);
}

alignment_t FastAligner::GetAlignment(const wordvec_t &source, const wordvec_t &target, Symmetrization symmetrization) {
    alignment_t forward = forwardModel->ComputeAlignment(source, target, &vocabulary);
    alignment_t backward = backwardModel->ComputeAlignment(source, target, &vocabulary);

    SymAlignment symmetrizer(source.size(), target.size());

    switch (symmetrization) {
        case GrowDiagonalFinalAnd:
            symmetrizer.Grow(forward, backward, true, true);
            break;
        case GrowDiagonal:
            symmetrizer.Grow(forward, backward, true, false);
            break;
        case Intersection:
            symmetrizer.Intersection(forward, backward);
            break;
        case Union:
            symmetrizer.Union(forward, backward);
            break;
    }

    return symmetrizer.ToAlignment();
}

void FastAligner::GetAlignments(const std::vector<std::pair<sentence_t, sentence_t>> &_batch,
                                std::vector<alignment_t> &outAlignments, Symmetrization symmetrization) {
    vector<pair<wordvec_t, wordvec_t>> batch;
    batch.resize(_batch.size());

#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < batch.size(); ++i) {
        vocabulary.Encode(_batch[i].first, batch[i].first);
        vocabulary.Encode(_batch[i].second, batch[i].second);
    }

    GetAlignments(batch, outAlignments, symmetrization);
}

void FastAligner::GetAlignments(const std::vector<std::pair<wordvec_t, wordvec_t>> &batch,
                                std::vector<alignment_t> &outAlignments, Symmetrization symmetrization) {
    vector<alignment_t> forwards;
    vector<alignment_t> backwards;

    forwardModel->ComputeAlignments(batch, forwards, &vocabulary);
    backwardModel->ComputeAlignments(batch, backwards, &vocabulary);

    outAlignments.resize(batch.size());

    SymAlignment symal;

    for (size_t i = 0; i < batch.size(); ++i) {
        symal.Reset(batch[i].first.size(), batch[i].second.size());

        alignment_t &forward = forwards[i];
        alignment_t &backward = backwards[i];

        switch (symmetrization) {
            case GrowDiagonalFinalAnd:
                symal.Grow(forward, backward, true, true);
                break;
            case GrowDiagonal:
                symal.Grow(forward, backward, true, false);
                break;
            case Intersection:
                symal.Intersection(forward, backward);
                break;
            case Union:
                symal.Union(forward, backward);
                break;
        }

        outAlignments[i] = symal.ToAlignment();
    }
}