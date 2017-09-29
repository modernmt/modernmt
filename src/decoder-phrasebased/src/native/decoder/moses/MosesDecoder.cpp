//
// Created by Davide  Caroselli on 03/12/15.
//

#include <mutex>
#include <unordered_map>

#include "MosesDecoder.h"

#include "TranslationTask.h"
#include "StaticData.h"
#include "ContextScope.h"
#include "Manager.h"
#include "IOWrapper.h"
#include "FF/StatefulFeatureFunction.h"

using namespace std;
using namespace mmt;
using namespace mmt::decoder;

inline void Explode(const string &text, vector<string> &output) {
    output.clear();
    stringstream inText(text);

    for (string word; inText >> word;)
        output.push_back(word);
}

inline void Explode(const string &text, vector<wid_t> &output) {
    output.clear();
    stringstream inText(text);

    for (wid_t word; inText >> word;)
        output.push_back(word);
}

inline void MapOOVs(vector<wid_t> &sentence, const vector<string> &tokens, unordered_map<wid_t, string> &oovs) {
    wid_t overflowId = (wid_t) -1;

    for (size_t i = 0; i < sentence.size(); ++i) {
        if (sentence[i] == kVocabularyUnknownWord) {
            wid_t id = overflowId--;
            sentence[i] = id;
            oovs[id] = tokens[i];
        }
    }
}

inline void RestoreOOVs(const vector<wid_t> &sentence, vector<string> &tokens,
                        const unordered_map<wid_t, string> &oovs) {
    for (size_t i = 0; i < tokens.size(); ++i) {
        if (tokens[i].empty()) {
            auto oov = oovs.find(sentence[i]);

            if (oov != oovs.end())
                tokens[i] = oov->second;
        }
    }
}

inline string Join(const vector<string> &sentence) {
    stringstream outText;
    for (size_t i = 0; i < sentence.size(); ++i) {
        if (i > 0)
            outText << ' ';
        outText << sentence[i];
    }

    return outText.str();
}

inline string Join(const vector<wid_t> &sentence) {
    stringstream outText;
    for (size_t i = 0; i < sentence.size(); ++i) {
        if (i > 0)
            outText << ' ';
        outText << sentence[i];
    }

    return outText.str();
}

inline string EncodeText(Vocabulary &vb, const string &text, unordered_map<wid_t, string> &oovs) {
    vector<string> tokens;
    Explode(text, tokens);

    vector<wid_t> sentence;
    vb.Lookup(tokens, sentence, false);

    MapOOVs(sentence, tokens, oovs);

    return Join(sentence);
}

inline string DecodeText(Vocabulary &vb, const string &text, const unordered_map<wid_t, string> &oovs) {
    vector<wid_t> sentence;
    Explode(text, sentence);

    vector<string> tokens;
    vb.ReverseLookup(sentence, tokens);

    RestoreOOVs(sentence, tokens, oovs);

    return Join(tokens);
}

namespace mmt {
    namespace decoder {

        class MosesDecoderImpl : public MosesDecoder {
            std::vector<feature_t> m_features;
            std::vector<IncrementalModel *> m_incrementalModels;
            Vocabulary vb;

        public:
            MosesDecoderImpl(Moses::Parameter &param, const string &vocabularyPath);

            virtual std::vector<feature_t> getFeatures() override;

            virtual std::vector<float> getFeatureWeights(feature_t &feature) override;

            virtual void
            setDefaultFeatureWeights(const std::map<std::string, std::vector<float>> &featureWeights) override;

            virtual translation_t translate(const std::string &text,
                                            const std::map<std::string, float> *translationContext,
                                            size_t nbestListSize) override;

            void DeliverUpdates(const std::vector<translation_unit_t> &batch) override;

            void DeliverDeletion(const updateid_t &id, const memory_t memory) override;

            unordered_map<stream_t, seqid_t> GetLatestUpdatesIdentifiers() override;
        };
    }
}

MosesDecoder *MosesDecoder::createInstance(const std::string &inifilePath, const std::string &vocabularyPath) {
    const char *inifile = inifilePath.c_str();
    const char *argv[2] = {"-f", inifile};

    Moses::Parameter params;

    if (!params.LoadParam(2, argv))
        return NULL;

    // initialize all "global" variables, which are stored in StaticData
    // note: this also loads models such as the language model, etc.
    if (!Moses::StaticData::LoadDataStatic(&params, "moses"))
        return NULL;

    return new MosesDecoderImpl(params, vocabularyPath);
}

MosesDecoderImpl::MosesDecoderImpl(Moses::Parameter &param, const string &vocabularyPath) : vb(vocabularyPath) {
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

translation_t MosesDecoderImpl::translate(const std::string &text,
                                          const std::map<std::string, float> *translationContext,
                                          size_t nbestListSize) {
    boost::shared_ptr<Moses::ContextScope> scope(
            new Moses::ContextScope(Moses::StaticData::Instance().GetAllWeightsNew()));

    if (translationContext != NULL) {
        boost::shared_ptr<std::map<std::string, float>> cw(new std::map<std::string, float>(*translationContext));
        scope->SetContextWeights(cw);
    }

    unordered_map<wid_t, string> oovs;

    // Execute translation request

    translation_request_t request;
    translation_t response;

    request.sourceSent = EncodeText(vb, text, oovs);
    request.nBestListSize = nbestListSize;

    // Translate

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

        response.text = DecodeText(vb, manager.GetBestTranslation(), oovs);
        response.alignment = manager.GetWordAlignment();

        if (manager.GetSource().options()->nbest.nbest_size) {
            manager.OutputNBest(response.hypotheses);

            std::vector<hypothesis_t> &hypotheses = response.hypotheses;
            vector<vector<wid_t>> batch(hypotheses.size());

            for (size_t i = 0; i < hypotheses.size(); ++i)
                Explode(hypotheses[i].text, batch[i]);

            vector<vector<string>> output;
            vb.ReverseLookup(batch, output);

            for (size_t i = 0; i < hypotheses.size(); ++i) {
                RestoreOOVs(batch[i], output[i], oovs);
                hypotheses[i].text = Join(output[i]);
            }

        }
    }

    return response;
}

void MosesDecoderImpl::DeliverUpdates(const std::vector<translation_unit_t> &batch) {
    vector<vector<string>> _sources(batch.size());
    vector<vector<string>> _targets(batch.size());

    for (size_t i = 0; i < batch.size(); ++i) {
        Explode(batch[i].source, _sources[i]);
        Explode(batch[i].target, _targets[i]);
    }

    vector<vector<wid_t>> sources;
    vector<vector<wid_t>> targets;

    vb.Lookup(_sources, sources, true);
    vb.Lookup(_targets, targets, true);

    for (size_t i = 0; i < batch.size(); ++i) {
        const translation_unit_t &unit = batch[i];
        vector<wid_t> &source = sources[i];
        vector<wid_t> &target = targets[i];

        for (auto it = m_incrementalModels.begin(); it != m_incrementalModels.end(); ++it) {
            IncrementalModel *model = *it;
            model->Add(unit.id, unit.memory, source, target, unit.alignment);
        }
    }
}

void MosesDecoderImpl::DeliverDeletion(const updateid_t &id, const memory_t memory) {
    for (auto it = m_incrementalModels.begin(); it != m_incrementalModels.end(); ++it) {
        IncrementalModel *model = *it;
        model->Delete(id, memory);
    }
}

unordered_map<stream_t, seqid_t> MosesDecoderImpl::GetLatestUpdatesIdentifiers() {
    unordered_map<stream_t, seqid_t> result;

    if (!m_incrementalModels.empty()) {
        result = m_incrementalModels[0]->GetLatestUpdatesIdentifier();

        for (size_t i = 1; i < m_incrementalModels.size(); ++i) {
            IncrementalModel *model = m_incrementalModels[i];

            unordered_map<stream_t, seqid_t> ids = model->GetLatestUpdatesIdentifier();
            for (auto id = ids.begin(); id != ids.end(); ++id) {
                auto other = result.find(id->first);

                if (other != result.end())
                    result[id->first] = std::min(id->second, other->second);
            }

            for (auto it = result.begin(); it != result.end();) {
                auto other = ids.find(it->first);

                if (other == ids.end()) {
                    it = result.erase(it);
                } else
                    it++;
            }
        }
    }

    return result;
}