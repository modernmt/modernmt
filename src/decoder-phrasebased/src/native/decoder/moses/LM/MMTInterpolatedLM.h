// $Id$

/***********************************************************************
Moses - factored phrase-based language decoder
Copyright (C) 2006 University of Edinburgh

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
***********************************************************************/

#ifndef moses_MMTInterpolatedLM_h
#define moses_MMTInterpolatedLM_h

#include <string>
#include <vector>

#include <mmt/ilm/InterpolatedLM.h>
#include <mmt/ilm/CachedLM.h>
#include <mmt/IncrementalModel.h>

#include "LM/SingleFactor.h"
#include "Factor.h"
#include "Hypothesis.h"
#include "TypeDef.h"
#include "Util.h"

#ifdef WITH_THREADS

#include <boost/thread.hpp>

#endif

using namespace mmt;
using namespace mmt::ilm;

namespace Moses {

    class MMTInterpolatedLM : public LanguageModelSingleFactor {
    public:
        typedef std::map<std::string, float> weightmap_t;

    protected:
        InterpolatedLM *m_lm;
        string m_modelPath;
        mmt::ilm::Options lm_options;

#ifdef WITH_THREADS
        boost::thread_specific_ptr<context_t> t_context_vec;
        boost::thread_specific_ptr<CachedLM> t_cached_lm;
#else
        boost::scoped_ptr<context_t> *t_context_vec;
#endif

    public:
        MMTInterpolatedLM(const std::string &line);

        ~MMTInterpolatedLM();

        void SetParameter(const std::string &key, const std::string &value) override;

        bool IsUseable(const FactorMask &mask) const override;

        void Load(AllOptions::ptr const &opts) override;

        const FFState *EmptyHypothesisState(const InputType &/*input*/) const override;

        virtual LMResult GetValue(const std::vector<const Word *> &contextFactor, State *finalState = NULL) const override;

        virtual void CalcScore(const Phrase &phrase, float &fullScore, float &ngramScore, size_t &oovCount) const override;

        virtual FFState *
        EvaluateWhenApplied(const Hypothesis &hypo, const FFState *ps, ScoreComponentCollection *out) const override;

        void InitializeForInput(ttasksptr const &ttask) override;

        void CleanUpAfterSentenceProcessing(const InputType &source) override;

        virtual mmt::IncrementalModel *GetIncrementalModel() const override {
            return m_lm;
        }

    private:

        void TransformPhrase(const Phrase &phrase, std::vector<wid_t> &phrase_vec, const size_t startGaps,
                             const size_t endGaps) const;

        void SetWordVector(const Hypothesis &hypo, std::vector<wid_t> &phrase_vec, const size_t startGaps,
                           const size_t endGaps, const size_t from, const size_t to) const;
    };

}

#endif
