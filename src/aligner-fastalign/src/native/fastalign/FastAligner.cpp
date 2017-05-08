//
// Created by Davide Caroselli on 04/09/16.
//

#include <symal/SymAlignment.h>
#include "FastAligner.h"
#include <thread>
#include <fstream>
#include "Model.h"
#ifdef _OPENMP
#include <omp.h>
#endif

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

Model *OpenModel(const string &path) {
    bool is_reverse;
    bool use_null;
    bool favor_diagonal;
    double prob_align_null;
    double diagonal_tension;

    ifstream in(path, ios::binary | ios::in);

    in.read((char *) &is_reverse, sizeof(bool));
    in.read((char *) &use_null, sizeof(bool));
    in.read((char *) &favor_diagonal, sizeof(bool));

    in.read((char *) &prob_align_null, sizeof(double));
    in.read((char *) &diagonal_tension, sizeof(double));

    size_t ttable_size;
    in.read((char *) &ttable_size, sizeof(size_t));

    builder_ttable_t *table = new builder_ttable_t;
    table->data.resize(ttable_size);

    while (true) {
        wid_t sourceWord;
        in.read((char *) &sourceWord, sizeof(wid_t));

        if (in.eof())
            break;

        size_t row_size;
        in.read((char *) &row_size, sizeof(size_t));

        unordered_map<wid_t, pair<double, double>> &row = table->data[sourceWord];
        row.reserve(row_size);

        for (size_t i = 0; i < row_size; ++i) {
            wid_t targetWord;
            double value;

            in.read((char *) &targetWord, sizeof(wid_t));
            in.read((char *) &value, sizeof(double));

            row[targetWord] = pair<double, double>(value, 0);
        }
    }

    return new Model(table, is_reverse, use_null, favor_diagonal, prob_align_null, diagonal_tension);
}

FastAligner *FastAligner::Open(const string &path, int threads) {
    Model *forward = OpenModel(path + kPathSeparator + kForwardModelFilename);
    Model *backward = OpenModel(path + kPathSeparator + kBackwardModelFilename);

    return new FastAligner(forward, backward, threads);
}

FastAligner::FastAligner(Model *forwardModel, Model *backwardModel, int threads)
        : forwardModel(forwardModel), backwardModel(backwardModel) {
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

float FastAligner::GetForwardProbability(wid_t source, wid_t target) {
    return (float) forwardModel->GetProbability(source, target);
}

float FastAligner::GetBackwardProbability(wid_t source, wid_t target) {
    return (float) backwardModel->GetProbability(target, source);
}

