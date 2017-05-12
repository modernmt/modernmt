// vim:tabstop=2
#include "PhraseDictionarySADB.h"
#include "StaticData.h"
#include "TranslationTask.h"

#define ParseWord(w) (boost::lexical_cast<wid_t>((w)))

using namespace std;
using namespace Moses;
using namespace mmt::sapt;

namespace Moses {

//used the constant of mmt::sapt to get entries of the vector cnt,
//used the cnstant of LRModel   for the probabilities
    void fill_lr_vec2(LRModel::ModelType mdl, const vector<size_t> &cnt, const float total, float *v) {
        if (mdl == LRModel::Monotonic) {
            float denom = log(total + 2);
            v[LRModel::M] = log(cnt[MonotonicOrientation] + 1.) - denom;
            v[LRModel::NM] = log(total - cnt[MonotonicOrientation] + 1) - denom;
        } else if (mdl == LRModel::LeftRight) {
            float denom = log(total + 2);
            v[LRModel::R] = log(cnt[MonotonicOrientation] + cnt[DiscontinuousRightOrientation] + 1.) - denom;
            v[LRModel::L] = log(cnt[SwapOrientation] + cnt[DiscontinuousLeftOrientation] + 1.) - denom;
        } else if (mdl == LRModel::MSD) {
            float denom = log(total + 3);
            v[LRModel::M] = log(cnt[MonotonicOrientation] + 1) - denom;
            v[LRModel::S] = log(cnt[SwapOrientation] + 1) - denom;
            v[LRModel::D] = log(cnt[DiscontinuousRightOrientation] +
                                cnt[DiscontinuousLeftOrientation] + 1) - denom;
        } else if (mdl == LRModel::MSLR) {
            float denom = log(total + 4);
            v[LRModel::M] = log(cnt[MonotonicOrientation] + 1) - denom;
            v[LRModel::S] = log(cnt[SwapOrientation] + 1) - denom;
            v[LRModel::DL] = log(cnt[DiscontinuousLeftOrientation] + 1) - denom;
            v[LRModel::DR] = log(cnt[DiscontinuousRightOrientation] + 1) - denom;
        } else
            UTIL_THROW2("Reordering type not recognized!");
    }

    void fill_lr_vec(LRModel::Direction const &dir,
                     LRModel::ModelType const &mdl,
                     const vector<size_t> &dfwd,
                     const vector<size_t> &dbwd,
                     vector<float> &v) {
        // how many distinct scores do we have?
        size_t num_scores = (mdl == LRModel::MSLR ? 4 : mdl == LRModel::MSD ? 3 : 2);
        size_t offset;
        if (dir == LRModel::Bidirectional) {
            offset = num_scores;
            num_scores *= 2;
        } else offset = 0;

        v.resize(num_scores);

        // determine the denominator
        float total = 0;
        for (size_t i = 0; i <= mmt::sapt::kTranslationOptionDistortionCount; ++i) {
            total += dfwd[i];
        }
        if (dir != LRModel::Forward) { // i.e., Backward or Bidirectional
            fill_lr_vec2(mdl, dbwd, total, &v[0]);
        }
        if (dir != LRModel::Backward) { // i.e., Forward or Bidirectional
            fill_lr_vec2(mdl, dfwd, total, &v[offset]);
        }
    };

    PhraseDictionarySADB::PhraseDictionarySADB(const std::string &line)
            : PhraseDictionary(line),
              m_lr_func(NULL) {
        m_numScoreComponents = 4;
        m_numTuneableComponents = m_numScoreComponents;
        ReadParameters();

        assert(m_input.size() == 1);
        assert(m_output.size() == 1);

        VERBOSE(3, GetScoreProducerDescription()
                << " PhraseDictionarySADB::PhraseDictionarySADB() m_filePath:|"
                << m_filePath << "|" << std::endl);
        VERBOSE(3, GetScoreProducerDescription()
                << " PhraseDictionarySADB::PhraseDictionarySADB() table-limit:|"
                << m_tableLimit << "|" << std::endl);
        VERBOSE(3, GetScoreProducerDescription()
                << " PhraseDictionarySADB::PhraseDictionarySADB() cache-size:|"
                << m_maxCacheSize << "|" << std::endl);
        VERBOSE(3, GetScoreProducerDescription()
                << " PhraseDictionarySADB::PhraseDictionarySADB() m_inputFactors:|"
                << m_inputFactors << "|" << std::endl);
        VERBOSE(3, GetScoreProducerDescription()
                << " PhraseDictionarySADB::PhraseDictionarySADB() m_outputFactors:|"
                << m_outputFactors << "|" << std::endl);
        VERBOSE(3, GetScoreProducerDescription()
                << " PhraseDictionarySADB::PhraseDictionarySADB() m_numScoreComponents:|"
                << m_numScoreComponents << "|" << std::endl);
        VERBOSE(3, GetScoreProducerDescription()
                << " PhraseDictionarySADB::PhraseDictionarySADB() m_input.size():|"
                << m_input.size() << "|" << std::endl);

        // caching for memory pt is pointless
        m_maxCacheSize = 0;
    }

    void PhraseDictionarySADB::Load(AllOptions::ptr const &opts) {
        m_options = opts;
        SetFeaturesToApply();
        m_pt = new mmt::sapt::PhraseTable(m_modelPath, pt_options);
    }


    PhraseDictionarySADB::~PhraseDictionarySADB() {
        delete m_pt;
    }


    void PhraseDictionarySADB::InitializeForInput(ttasksptr const &ttask) {
        //todo: do we need this cache?
        ReduceCache();

#ifdef TRACE_CACHE
        m_lmtb->sentence_id++;
#endif
        // we assume here that translation is run in one single thread for each ttask
        // (no parallelization at a finer granularity involving PhraseDictionarySADB)

        // This function is called prior to actual translation and allows the class
        // to set up thread-specific information such as context weights

        // DO NOT modify members of 'this' here. We are being called from different
        // threads, and there is no locking here.
        SPTR<weightmap_t const> weights = ttask->GetScope()->GetContextWeights();

        if (weights) {
            context_t *context_vec = new context_t;

            for (weightmap_t::const_iterator it = weights->begin(); it != weights->end(); ++it) {
                context_vec->push_back(cscore_t(ParseWord(it->first), it->second));
            }

            t_context_vec.reset(context_vec);
        }

        if (m_lr_func_name.size() && m_lr_func == NULL) {
            FeatureFunction *lr = &FeatureFunction::FindFeatureFunction(m_lr_func_name);
            m_lr_func = dynamic_cast<LexicalReordering *>(lr);
            UTIL_THROW_IF2(lr == NULL,
                           "FF " << m_lr_func_name << " does not seem to be a lexical reordering function!");
            // todo: verify that lr_func implements a hierarchical reordering model
        }
    }

    inline vector<wid_t> PhraseDictionarySADB::ParsePhrase(const Phrase &phrase) const {
        vector<wid_t> result(phrase.GetSize());

        for (size_t i = 0; i < phrase.GetSize(); i++) {
            result[i] = ParseWord(phrase.GetWord(i).GetString(m_input, false));
        }

        return result;
    }

    void PhraseDictionarySADB::GetTargetPhraseCollectionBatch(ttasksptr const &ttask,
                                                              const InputPathList &inputPathQueue) const {
        Phrase sourceSentence(ttask->GetSource()->GetSubString(Range(0, ttask->GetSource()->GetSize() - 1)));

        context_t *context = t_context_vec.get();

        vector<wid_t> sentence = ParsePhrase(sourceSentence);
        translation_table_t ttable = m_pt->GetAllTranslationOptions(sentence, context);
        for (auto inputPath = inputPathQueue.begin(); inputPath != inputPathQueue.end(); ++inputPath) {
            vector<wid_t> phrase = ParsePhrase((*inputPath)->GetPhrase());

            auto options = ttable.find(phrase);
            if (options != ttable.end()) {
                TargetPhraseCollection::shared_ptr targetPhrases = MakeTargetPhraseCollection(ttask,
                                                                                              (*inputPath)->GetPhrase(),
                                                                                              options->second);
                (*inputPath)->SetTargetPhrases(*this, targetPhrases, NULL);
            }
        }
    }

    TargetPhraseCollection::shared_ptr
    PhraseDictionarySADB::MakeTargetPhraseCollection(ttasksptr const &ttask, Phrase const &sourcePhrase,
                                                     const vector<mmt::sapt::TranslationOption> &options) const {
        TargetPhraseCollection *tpc = new TargetPhraseCollection();

        auto target_options_it = options.begin();


        //transform the SAPT translation Options into Moses Target Phrases
        for (target_options_it = options.begin();
             target_options_it != options.end(); ++target_options_it) {

            TargetPhrase *tp = new TargetPhrase(ttask, this);
            for (auto word_it = target_options_it->targetPhrase.begin();
                 word_it != target_options_it->targetPhrase.end(); ++word_it) {
                Word w;
                w.CreateFromString(Output, m_output, to_string(*word_it), false);
                tp->AddWord(w);
            }
            std::set<std::pair<size_t, size_t> > aln;
            for (auto alignment_it = target_options_it->alignment.begin();
                 alignment_it != target_options_it->alignment.end(); ++alignment_it) {
                aln.insert(std::make_pair(size_t(alignment_it->first), size_t(alignment_it->second)));
            }

            tp->SetAlignTerm(aln);
            tp->GetScoreBreakdown().Assign(this, target_options_it->scores);
            tpc->Add(tp);
            // Evaluate with all features that can be computed using available factors
            tp->EvaluateInIsolation(sourcePhrase, m_featuresToApply);


            if (m_lr_func) {
                SPTR<Scores> scores(new Scores());

                Moses::fill_lr_vec(m_lr_func->GetModel().GetDirection(),
                                   m_lr_func->GetModel().GetModelType(),
                                   target_options_it->orientations.forward,
                                   target_options_it->orientations.backward,
                                   *scores);

                tp->SetExtraScores(m_lr_func, scores);
            }
        }

        if (m_tableLimit) tpc->Prune(true, m_tableLimit);
        else tpc->Prune(true, tpc->GetSize());

        TargetPhraseCollection::shared_ptr tpc_sp;
        tpc_sp.reset(tpc);

        return tpc_sp;
    }

    void PhraseDictionarySADB::SetParameter(const std::string &key, const std::string &value) {

        if (key == "path") {
            m_modelPath = Scan<std::string>(value);
            VERBOSE(3, "m_modelPath:" << m_modelPath << std::endl);
        } else if (key == "sample-limit") {
            pt_options.samples = Scan<int>(value);
            VERBOSE(3, "pt_options.sample:" << pt_options.samples << std::endl);
        } else if (key == "lr-func") {
            m_lr_func_name = Scan<std::string>(value);
            VERBOSE(3, "m_lr_func_name:" << m_lr_func_name << std::endl);
        } else {
            PhraseDictionary::SetParameter(key, value);
        }
    }

    ChartRuleLookupManager *PhraseDictionarySADB::CreateRuleLookupManager(const ChartParser &parser,
                                                                          const ChartCellCollectionBase &cellCollection,
                                                                          std::size_t /*maxChartSpan*/) {
        throw "CreateRuleLookupManager is currently not supported in Mmsapt!";
    }


    TO_STRING_BODY(PhraseDictionarySADB);

// friend
    ostream &operator<<(ostream &out, const PhraseDictionarySADB &phraseDict) {
        return out;
    }

}
