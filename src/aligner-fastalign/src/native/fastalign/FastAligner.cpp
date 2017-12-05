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

void
FastAligner::GetScores(const std::vector<std::pair<sentence_t, sentence_t>> &_batch, std::vector<double> &outScores) {
    vector<pair<wordvec_t, wordvec_t>> batch;
    batch.resize(_batch.size());

#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < batch.size(); ++i) {
        vocabulary->Encode(_batch[i].first, batch[i].first);
        vocabulary->Encode(_batch[i].second, batch[i].second);
    }

    GetScores(batch, outScores);
}

void FastAligner::GetScores(const std::vector<std::pair<wordvec_t, wordvec_t>> &batch, std::vector<double> &outScores) {
    vector<double> forwards;
    vector<double> backwards;

    forwardModel->ComputeScores(batch, forwards);
    backwardModel->ComputeScores(batch, backwards);

    outScores.resize(batch.size());

#pragma omp parallel for schedule(dynamic)
    for (size_t i = 0; i < batch.size(); ++i) {
        outScores[i] = (forwards[i] + backwards[i]) / 2;
    }
}

alignment_t FastAligner::GetAlignment(const sentence_t &_source, const sentence_t &_target,
                                      Symmetrization symmetrization) {
    wordvec_t source, target;
    vocabulary->Encode(_source, source);
    vocabulary->Encode(_target, target);

    return GetAlignment(source, target, symmetrization);
}

alignment_t FastAligner::GetAlignment(const wordvec_t &source, const wordvec_t &target, Symmetrization symmetrization) {
    alignment_t forward = forwardModel->ComputeAlignment(source, target);
    alignment_t backward = backwardModel->ComputeAlignment(source, target);

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
        vocabulary->Encode(_batch[i].first, batch[i].first);
        vocabulary->Encode(_batch[i].second, batch[i].second);
    }

    GetAlignments(batch, outAlignments, symmetrization);
}

void FastAligner::GetAlignments(const std::vector<std::pair<wordvec_t, wordvec_t>> &batch,
                                std::vector<alignment_t> &outAlignments, Symmetrization symmetrization) {
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