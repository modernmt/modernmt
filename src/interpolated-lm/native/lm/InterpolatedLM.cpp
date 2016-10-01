//
// Created by Davide Caroselli on 27/07/16.
//

#include <iostream>
#include "InterpolatedLM.h"
#include "AdaptiveLM.h"
#include "StaticLM.h"
#include <boost/filesystem.hpp>

namespace fs = boost::filesystem;

using namespace std;
using namespace mmt::ilm;

namespace mmt {
    namespace ilm {

        // The state of InterpolatedLM is always the combination of the static lm state and
        // the adaptive lm state. This ensures the consistency even in the eventuality
        // that the static lm does not contain all the n-grams of the adaptive lm.
        struct ILMHistoryKey : public HistoryKey {

            const HistoryKey *alm_key;
            const HistoryKey *slm_key;

            ILMHistoryKey(HistoryKey *alm_key, HistoryKey *slm_key) : alm_key(alm_key), slm_key(slm_key) {}

            ~ILMHistoryKey() {
                if (alm_key != NULL)
                    delete alm_key;
                if (slm_key != NULL)
                    delete slm_key;
            }

            size_t hash() const override {
                if (alm_key == NULL) {
                    return slm_key == NULL ? 0 : slm_key->hash();
                } else if (slm_key == NULL) {
                    return alm_key->hash();
                } else {
                    return ((alm_key->hash() * 8978948897894561157ULL) ^ (slm_key->hash() * 17894857484156487943ULL));
                }
            }

            bool operator==(const HistoryKey &other) const override {
                ILMHistoryKey &o = (ILMHistoryKey &) other;
                if (alm_key == NULL)
                    return o.alm_key == NULL;
                if (slm_key == NULL)
                    return o.slm_key == NULL;

                if (o.alm_key == NULL)
                    return false;
                if (o.slm_key == NULL)
                    return false;

                return (*alm_key) == (*o.alm_key) && (*slm_key) == (*o.slm_key);
            }

            virtual size_t length() const override {
                if (alm_key == NULL)
                    return slm_key == NULL ? 0 : slm_key->length();
                else if (slm_key == NULL)
                    return alm_key->length();
                else
                    return max(alm_key->length(), slm_key->length());
            }
        };

    }
}

struct InterpolatedLM::ilm_private {
    AdaptiveLM *alm = nullptr;
    StaticLM *slm = nullptr;

    bool is_alm_active = false;
    double log_alm_weight = 0.0;
    bool is_slm_active = false;
    double log_slm_weight = 0.0;
};

InterpolatedLM::InterpolatedLM(const string &modelPath, const Options &options) {
    if (options.adaptivity_ratio < 0 || options.adaptivity_ratio > 1.)
        throw invalid_argument("Invalid adaptivity_ratio");

    fs::path modelDir(modelPath);

    if (!fs::is_directory(modelDir))
        throw invalid_argument("Invalid model path: " + modelPath);

    fs::path slmFile = fs::absolute(modelDir / fs::path("background.slm"));
    fs::path almDir = fs::absolute(modelDir / fs::path("foreground.alm"));

    if (!fs::is_regular_file(slmFile))
        throw invalid_argument("Invalid model path, missing file: " + slmFile.string());

    if (!fs::is_directory(almDir))
        fs::create_directory(almDir);

    self = new ilm_private();

    if (options.adaptivity_ratio == 1) {
        self->is_alm_active = true;
        self->is_slm_active = false;
    } else if (options.adaptivity_ratio == 0) {
        self->is_alm_active = false;
        self->is_slm_active = true;
    } else {
        self->is_alm_active = true;
        self->is_slm_active = true;

        self->log_alm_weight = log(options.adaptivity_ratio);
        self->log_slm_weight = log(1.f - options.adaptivity_ratio);
    }

    if (self->is_alm_active)
        self->alm = new AdaptiveLM(almDir.string(), options.order, options.update_buffer_size,
                                   options.update_max_delay);

    if (self->is_slm_active)
        self->slm = new StaticLM(slmFile.string());
}

InterpolatedLM::~InterpolatedLM() {
    if (self->alm)
        delete self->alm;
    if (self->slm)
        delete self->slm;
    delete self;
}

HistoryKey *InterpolatedLM::MakeHistoryKey(const vector<wid_t> &phrase) const {
    return new ILMHistoryKey(self->alm ? self->alm->MakeHistoryKey(phrase) : NULL,
                                 self->slm ? self->slm->MakeHistoryKey(phrase) : NULL);
}

HistoryKey *InterpolatedLM::MakeEmptyHistoryKey() const {
    return new ILMHistoryKey(self->alm ? self->alm->MakeEmptyHistoryKey() : NULL,
                                 self->slm ? self->slm->MakeEmptyHistoryKey() : NULL);
}

bool InterpolatedLM::IsOOV(const context_t *context, const wid_t word) const {
    bool use_slm = self->is_slm_active;
    bool use_alm = self->is_alm_active && context != NULL && !context->empty();

    if (use_slm && !self->slm->IsOOV(context, word))
        return false;

    if (use_alm && !self->alm->IsOOV(context, word))
        return false;

    return true;
}

static inline double log_sum(double log_a, double log_b) {
    if (log_a < log_b) {
        return (double) (log_b + log1p(exp(log_a - log_b)));
    } else {
        return (double) (log_a + log1p(exp(log_b - log_a)));
    }
}

float InterpolatedLM::ComputeProbability(const wid_t word, const HistoryKey *historyKey, const context_t *context,
                                  HistoryKey **outHistoryKey, void *cache) const {
    const ILMHistoryKey *inKey = static_cast<const ILMHistoryKey *> (historyKey);
    assert(inKey != NULL);

    HistoryKey *alm_key = NULL;
    HistoryKey *slm_key = NULL;

    double result = kNaturalLogZeroProbability;
    float slm_probability = kNaturalLogZeroProbability;
    float alm_probability = kNaturalLogZeroProbability;

    bool use_slm = self->is_slm_active;
    bool use_alm = self->is_alm_active && context != NULL && !context->empty();

    if (use_slm)
        slm_probability = self->slm->ComputeProbability(word, inKey->slm_key, context, outHistoryKey ? &slm_key : NULL);
    if (use_alm)
        alm_probability = self->alm->ComputeProbability(word, inKey->alm_key, context, outHistoryKey ? &alm_key : NULL,
                                                        (AdaptiveLMCache *) cache);

    if (use_slm && use_alm) // we defined slm_weight == 1.0 - alm_weight
        result = log_sum(self->log_slm_weight + slm_probability, self->log_alm_weight + alm_probability);
    else if (use_slm) // we force slm_weight = 1.0
        result = slm_probability;
    else if (use_alm) // we force alm_weight = 1.0
        result = alm_probability;

    if (outHistoryKey)
        *outHistoryKey = new ILMHistoryKey(alm_key, slm_key);

    return (float) result;
}

void InterpolatedLM::Add(const updateid_t &id, const domain_t domain, const vector<wid_t> &source, const vector<wid_t> &target,
                  const alignment_t &alignment) {
    if (self->is_alm_active)
        self->alm->Add(id, domain, source, target, alignment);
}

vector<mmt::updateid_t> InterpolatedLM::GetLatestUpdatesIdentifier() {
    return self->alm->GetLatestUpdatesIdentifier();
}

void InterpolatedLM::NormalizeContextMap(context_t *context) {
    if (self->is_alm_active)
        self->alm->NormalizeContext(context);
}
