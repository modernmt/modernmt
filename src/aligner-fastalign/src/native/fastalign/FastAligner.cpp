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

const Vocabulary *LoadVocabulary(const string &path) {
    fs::path filename = fs::absolute(fs::path(path) / fs::path("model.voc"));
    if (!fs::is_regular(filename))
        throw invalid_argument("File not found: " + filename.string());

    return new Vocabulary(filename.string(), true, false);
}

FastAligner::FastAligner(const string &path, int threads) : vocabulary(LoadVocabulary(path)) {
    fs::path model_filename = fs::absolute(fs::path(path) / fs::path("model.dat"));
    if (!fs::is_regular(model_filename))
        throw invalid_argument("File not found: " + model_filename.string());

    BidirectionalModel::Open(model_filename.string(), &forwardModel, &backwardModel);

    this->threads = threads > 0 ? threads : (int) thread::hardware_concurrency();
#ifdef _OPENMP
    omp_set_dynamic(0);
    omp_set_num_threads(this->threads);
#endif
}

FastAligner::~FastAligner() {
    delete forwardModel;
    delete backwardModel;
    delete vocabulary;
}

alignment_t
FastAligner::GetAlignment(const vector<wid_t> &source, const vector<wid_t> &target, SymmetrizationStrategy strategy) {
    alignment_t forward = forwardModel->ComputeAlignment(source, target);
    alignment_t backward = backwardModel->ComputeAlignment(source, target);

    SymAlignment symmetrizer(source.size(), target.size());

    switch (strategy) {
        case GrowDiagonalFinalAndStrategy:
            symmetrizer.Grow(forward, backward, true, true);
            break;
        case GrowDiagonalStrategy:
            symmetrizer.Grow(forward, backward, true, false);
            break;
        case IntersectionStrategy:
            symmetrizer.Intersection(forward, backward);
            break;
        case UnionStrategy:
            symmetrizer.Union(forward, backward);
            break;
    }

    return symmetrizer.ToAlignment();
}

void
FastAligner::GetAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch, vector<alignment_t> &outAlignments,
                           SymmetrizationStrategy strategy) {
    vector<alignment_t> forwards;
    vector<alignment_t> backwards;

    forwardModel->ComputeAlignments(batch, forwards);
    backwardModel->ComputeAlignments(batch, backwards);

    outAlignments.resize(batch.size());

    vector<SymAlignment> symals((size_t) threads);

#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < batch.size(); ++i) {
#ifdef _OPENMP
        SymAlignment &symal = symals[omp_get_thread_num()];
#else
        SymAlignment &symal = symals[0];
#endif


        symal.Reset(batch[i].first.size(), batch[i].second.size());

        alignment_t &forward = forwards[i];
        alignment_t &backward = backwards[i];

        switch (strategy) {
            case GrowDiagonalFinalAndStrategy:
                symal.Grow(forward, backward, true, true);
                break;
            case GrowDiagonalStrategy:
                symal.Grow(forward, backward, true, false);
                break;
            case IntersectionStrategy:
                symal.Intersection(forward, backward);
                break;
            case UnionStrategy:
                symal.Union(forward, backward);
                break;
        }

        outAlignments[i] = symal.ToAlignment();
    }
}

alignment_t FastAligner::GetForwardAlignment(const vector<wid_t> &source, const vector<wid_t> &target) {
    return forwardModel->ComputeAlignment(source, target);
}

void FastAligner::GetForwardAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch,
                                       vector<alignment_t> &outAlignments) {
    forwardModel->ComputeAlignments(batch, outAlignments);
}

alignment_t FastAligner::GetBackwardAlignment(const vector<wid_t> &source, const vector<wid_t> &target) {
    return backwardModel->ComputeAlignment(source, target);
}

void FastAligner::GetBackwardAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch,
                                        vector<alignment_t> &outAlignments) {
    backwardModel->ComputeAlignments(batch, outAlignments);
}

