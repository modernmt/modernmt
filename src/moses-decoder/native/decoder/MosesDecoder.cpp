//
// Created by Davide  Caroselli on 03/12/15.
//

#include <mutex>
#include <unordered_map>

#include "decoder/MosesDecoder.h"

#include "TranslationTask.h"
#include "StaticData.h"
#include "ContextScope.h"
#include "Manager.h"
#include "IOWrapper.h"
#include "FF/StatefulFeatureFunction.h"

using namespace std;
using namespace mmt;
using namespace mmt::decoder;

namespace mmt {
    namespace decoder {

        class MosesDecoderImpl : public MosesDecoder {
            std::mutex m_sessionsMutex;
            unordered_map<uint64_t, boost::shared_ptr<Moses::ContextScope>> m_sessions;
            std::vector<feature_t> m_features;
            std::vector<IncrementalModel *> m_incrementalModels;

            uint64_t createSession(const std::map<std::string, float> *translationContext = NULL,
                                  const std::map<std::string, std::vector<float>> *featureWeights = NULL);
        public:

            MosesDecoderImpl(Moses::Parameter &param);

            virtual std::vector<feature_t> getFeatures() override;

            virtual std::vector<float> getFeatureWeights(feature_t &feature) override;

            virtual void
            setDefaultFeatureWeights(const std::map<std::string, std::vector<float>> &featureWeights) override;

            virtual int64_t openSession(const std::map<std::string, float> &translationContext,
                                        const std::map<std::string, std::vector<float>> *featureWeights = NULL) override;

            virtual void closeSession(uint64_t session) override;

            virtual translation_t translate(const std::string &text, uint64_t session,
                                            const std::map<std::string, float> *translationContext,
                                            size_t nbestListSize) override;

            virtual const vector<IncrementalModel *> &GetIncrementalModels() const override;
        };
    }
}

MosesDecoder *MosesDecoder::createInstance(const char *inifile, Aligner *aligner, Vocabulary *vocabulary) {
    const char *argv[2] = {"-f", inifile};

    Moses::Parameter params;

    if (!params.LoadParam(2, argv))
        return NULL;

    // initialize all "global" variables, which are stored in StaticData
    // note: this also loads models such as the language model, etc.
    if (!Moses::StaticData::LoadDataStatic(&params, "moses", aligner, vocabulary))
        return NULL;

    return new MosesDecoderImpl(params);
}

MosesDecoderImpl::MosesDecoderImpl(Moses::Parameter &param) : m_features() {
    const std::vector<const Moses::StatelessFeatureFunction *> &slf = Moses::StatelessFeatureFunction::GetStatelessFeatureFunctions();
    for (size_t i = 0; i < slf.size(); ++i) {
        const Moses::FeatureFunction *feature = slf[i];
        feature_t f;
        f.name = feature->GetScoreProducerDescription();
        f.stateless = feature->IsStateless();
        f.tunable = feature->IsTuneable();
        f.ptr = (void *) feature;

        m_features.push_back(f);
        mmt::IncrementalModel *model = feature->GetIncrementalModel();
        if (model)
            m_incrementalModels.push_back(model);
    }

    const std::vector<const Moses::StatefulFeatureFunction *> &sff = Moses::StatefulFeatureFunction::GetStatefulFeatureFunctions();
    for (size_t i = 0; i < sff.size(); ++i) {
        const Moses::FeatureFunction *feature = sff[i];

        feature_t f;
        f.name = feature->GetScoreProducerDescription();
        f.stateless = feature->IsStateless();
        f.tunable = feature->IsTuneable();
        f.ptr = (void *) feature;

        m_features.push_back(f);
        mmt::IncrementalModel *model = feature->GetIncrementalModel();
        if (model)
            m_incrementalModels.push_back(model);
    }
}

std::vector<feature_t> MosesDecoderImpl::getFeatures() {
    return m_features;
}

std::vector<float> MosesDecoderImpl::getFeatureWeights(feature_t &_feature) {
    Moses::FeatureFunction *feature = (Moses::FeatureFunction *) _feature.ptr;
    std::vector<float> weights;

    if (feature->IsTuneable()) {
        weights = Moses::StaticData::Instance().GetAllWeightsNew().GetScoresForProducer(feature);

        for (size_t i = 0; i < feature->GetNumScoreComponents(); ++i) {
            if (!feature->IsTuneableComponent(i)) {
                weights[i] = UNTUNEABLE_COMPONENT;
            }
        }
    }

    return weights;
}

void MosesDecoderImpl::setDefaultFeatureWeights(const std::map<std::string, std::vector<float>> &featureWeights) {
    Moses::StaticData::InstanceNonConst().SetAllWeights(Moses::ScoreComponentCollection::FromWeightMap(featureWeights));
}


int64_t MosesDecoderImpl::openSession(const std::map<std::string, float> &translationContext,
                                      const std::map<std::string, std::vector<float>> *featureWeights)
{
    return createSession(&translationContext, featureWeights);
}

uint64_t MosesDecoderImpl::createSession(const std::map<std::string, float> *translationContext,
                                         const std::map<std::string, std::vector<float>> *featureWeights)
{
    boost::shared_ptr<Moses::ContextScope> scope(new Moses::ContextScope(Moses::StaticData::Instance().GetAllWeightsNew()));

    if(translationContext != NULL) {
        boost::shared_ptr<std::map<std::string, float> > cw(new std::map<std::string, float>(*translationContext));
        scope->SetContextWeights(cw);
    }

    if (featureWeights != NULL) {
        boost::shared_ptr<std::map<std::string, std::vector<float> > > fw(
            new std::map<std::string, std::vector<float> >(*featureWeights));
        scope->SetFeatureWeights(fw);
    }

    uint64_t session_id;
    {
        std::lock_guard<std::mutex> lock(m_sessionsMutex);
        // start with session ID 1 (when passed to translate(), session ID 0 means 'no session')
        session_id = m_sessions.size() + 1;
        m_sessions.insert(std::make_pair(session_id, scope));
    }
    return session_id;
}

void MosesDecoderImpl::closeSession(uint64_t session) {
    std::lock_guard<std::mutex> lock(m_sessionsMutex);
    m_sessions.erase(session);
}

static void DoTranslate(translation_request_t const& request, boost::shared_ptr<Moses::ContextScope> scope, translation_t &result) {
    boost::shared_ptr<Moses::AllOptions> opts(new Moses::AllOptions());
    *opts = *Moses::StaticData::Instance().options();

    if (request.nBestListSize > 0) {
        opts->nbest.only_distinct = true;
        opts->nbest.nbest_size = request.nBestListSize;
        opts->nbest.enabled = true;
    }

    boost::shared_ptr<Moses::InputType> source(new Moses::Sentence(opts, 0, request.sourceSent));
    boost::shared_ptr<Moses::IOWrapper> ioWrapperNone;

    boost::shared_ptr<Moses::TranslationTask> ttask = Moses::TranslationTask::create(source, ioWrapperNone, scope);

    // note: ~Manager() must run while we still own TranslationTask (because it only has a weak_ptr)
    {
        Moses::Manager manager(ttask);
        manager.Decode();

        result.text = manager.GetBestTranslation();
        result.alignment = manager.GetWordAlignment();

        if (manager.GetSource().options()->nbest.nbest_size)
            manager.OutputNBest(result.hypotheses);
    }
}

translation_t MosesDecoderImpl::translate(const std::string &text, uint64_t session,
                                          const std::map<std::string, float> *translationContext,
                                          size_t nbestListSize) {

    // Retrieve the ContextScope of the session, or create a temporary one

    //bool have_session = (session != 0);
    bool have_session;
    boost::shared_ptr<Moses::ContextScope> scope;
    {
        std::lock_guard<std::mutex> lock(m_sessionsMutex);
        auto it = m_sessions.find(session);
        have_session = (it != m_sessions.end());
        if(have_session) {
            UTIL_THROW_IF2(translationContext != nullptr, "translate(): you cannot specify both session and translationContext");
            scope = it->second;
        }
    }
    if(!have_session) {
        // note: createSession() uses a lock.
        session = createSession(translationContext, NULL);
        {
            std::lock_guard<std::mutex> lock(m_sessionsMutex);
            scope = m_sessions.at(session);
        }
    }

    // Execute translation request

    translation_request_t request;
    translation_t response;

    request.sourceSent = text;
    request.nBestListSize = nbestListSize;

    DoTranslate(request, scope, response);

    if(!have_session)
        closeSession(session);

    return response;
}

const vector<IncrementalModel *> &MosesDecoderImpl::GetIncrementalModels() const {
    return m_incrementalModels;
}