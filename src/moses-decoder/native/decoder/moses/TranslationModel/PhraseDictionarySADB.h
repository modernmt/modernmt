//
// Created by Nicola Bertoldi on 27/09/16.
//

#ifndef PHRASETABLE_SADB_H
#define PHRASETABLE_SADB_H
#pragma once

#include "PhraseDictionary.h"
#include "sapt/PhraseTable.h"
#include <mmt/sentence.h>
#include <mmt/IncrementalModel.h>
#include "moses/Hypothesis.h"
#include "moses/TypeDef.h"
#include "moses/Util.h"

#include "moses/FF/LexicalReordering/LexicalReordering.h"
#ifdef WITH_THREADS

#include <boost/thread.hpp>

#endif

namespace Moses {
    using namespace mmt;
    using namespace mmt::sapt;

    class ChartParser;

    class ChartCellCollectionBase;

    class ChartRuleLookupManager;

    class PhraseDictionarySADB : public PhraseDictionary {
        friend std::ostream &operator<<(std::ostream &, const PhraseDictionarySADB &);

    public:
        typedef std::map<std::string, float> weightmap_t;

        PhraseDictionarySADB(const std::string &line);

        ~PhraseDictionarySADB();

        void Load(AllOptions::ptr const &opts) override;

        void InitializeForInput(ttasksptr const &ttask) override;

        virtual void
        GetTargetPhraseCollectionBatch(ttasksptr const &ttask, InputPathList const &inputPathQueue) const override;

        ChartRuleLookupManager *
        CreateRuleLookupManager(const ChartParser &, const ChartCellCollectionBase &, std::size_t) override;

        TO_STRING();

        void SetParameter(const std::string &key, const std::string &value) override;

        virtual mmt::IncrementalModel *GetIncrementalModel() const override {
            return m_pt;
        }

    protected:

#ifdef WITH_THREADS
        boost::thread_specific_ptr<ttasksptr> m_ttask;
        boost::thread_specific_ptr<context_t> t_context_vec;
#else
        boost::scoped_ptr<context_t> *t_context_vec;
#endif

    private:
        PhraseTable *m_pt;
        string m_modelPath;
        mmt::sapt::Options pt_options;
        std::vector<FactorType> m_ifactor, m_ofactor;

        LexicalReordering* m_lr_func; // associated lexical reordering function
        std::string m_lr_func_name; // name of associated lexical reordering function

        inline vector<wid_t> ParsePhrase(const Phrase &phrase) const;

        TargetPhraseCollection::shared_ptr
        MakeTargetPhraseCollection(ttasksptr const &ttask, Phrase const &sourcePhrase,
                                   const vector<mmt::sapt::TranslationOption> &options) const;

    };

} // namespace Moses

#endif //PHRASETABLE_SADB_H
