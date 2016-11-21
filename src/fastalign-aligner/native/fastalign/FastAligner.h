//
// Created by Davide Caroselli on 04/09/16.
//

#ifndef FASTALIGN_ALIGNER_H
#define FASTALIGN_ALIGNER_H

#include <mmt/aligner/Aligner.h>
#include <string>

namespace mmt {
    namespace fastalign {

        class FastAligner : public Aligner {
        public:

            static const string kForwardModelFilename;
            static const string kBackwardModelFilename;

            FastAligner(AlignerModel *forwardModel, AlignerModel *backwardModel, int threads = 0);

            static FastAligner *Open(const string &path, int threads = 0);

            virtual alignment_t GetAlignment(const vector<wid_t> &source, const vector<wid_t> &target,
                                             SymmetrizationStrategy strategy) override;

            virtual void
            GetAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch, vector<alignment_t> &outAlignments,
                          SymmetrizationStrategy strategy) override;

            virtual alignment_t GetForwardAlignment(const vector<wid_t> &source, const vector<wid_t> &target) override;

            virtual void GetForwardAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch,
                                              vector<alignment_t> &outAlignments) override;

            virtual alignment_t GetBackwardAlignment(const vector<wid_t> &source, const vector<wid_t> &target) override;

            virtual void GetBackwardAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch,
                                               vector<alignment_t> &outAlignments) override;

            // P(target | source)
            virtual float GetForwardProbability(wid_t source, wid_t target) override;

            // P(source | target)
            virtual float GetBackwardProbability(wid_t source, wid_t target) override;

            // P(NULL | source)
            virtual float GetSourceNullProbability(wid_t source) override {
                return GetForwardProbability(source, kAlignerNullWord);
            };

            // P(NULL | target)
            virtual float GetTargetNullProbability(wid_t target) override {
                return GetForwardProbability(kAlignerNullWord, target);
            };

            virtual ~FastAligner() override;

        private:
            AlignerModel *forwardModel;
            AlignerModel *backwardModel;

            int threads;
        };

    }
}


#endif //FASTALIGN_ALIGNER_H
