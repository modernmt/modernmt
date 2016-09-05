//
// Created by Davide Caroselli on 04/09/16.
//

#include <symal/SymAlignment.h>
#include "FastAligner.h"
#include <thread>
#include <omp.h>

const string kPathSeparator =
#ifdef _WIN32
        "\\";
#else
        "/";
#endif

using namespace mmt;
using namespace mmt::fastalign;

const string FastAligner::kForwardModelFilename = "forward.fam";
const string FastAligner::kBackwardModelFilename = "backward.fam";

FastAligner *FastAligner::Open(const string &path, int threads) {
    Model *forward = Model::Open(path + kPathSeparator + kForwardModelFilename);
    Model *backward = Model::Open(path + kPathSeparator + kBackwardModelFilename);

    return new FastAligner(forward, backward, threads);
}

FastAligner::FastAligner(Model *forwardModel, Model *backwardModel, int threads) : forwardModel(forwardModel),
                                                                                   backwardModel(backwardModel) {
    this->threads = threads > 0 ? threads : (int) thread::hardware_concurrency();

    omp_set_dynamic(0);
    omp_set_num_threads(this->threads);
}

FastAligner::~FastAligner() {
    delete forwardModel;
    delete backwardModel;
}

alignment_t
FastAligner::GetAlignment(const vector<wid_t> &source, const vector<wid_t> &target, SymmetrizationStrategy strategy) {
    alignment_t forward = forwardModel->ComputeAlignment(source, target);
    alignment_t backward = backwardModel->ComputeAlignment(source, target);

    SymAlignment symmetrizer(source.size(), target.size());

    switch (strategy) {
        case GrowDiagonalFinalStrategy:
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
        SymAlignment &symal = symals[omp_get_thread_num()];

        symal.Reset(batch[i].first.size(), batch[i].second.size());

        alignment_t &forward = forwards[i];
        alignment_t &backward = backwards[i];

        switch (strategy) {
            case GrowDiagonalFinalStrategy:
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

