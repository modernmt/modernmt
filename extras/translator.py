import os

from cli import IllegalArgumentException
from cli.evaluation import MMTTranslator, _EvaluationResult, TranslateError, BLEUScore
from cli.libs import fileutils
from cli.mmt.processing import XMLEncoder


# similar to class Evaluator above. Alternatively we could have added a flag there, to call corpus.copy(dest_path)
# but we also don't want to run all eval metrics, and want to support missing reference on the source, ...
class BatchTranslator:
    def __init__(self, node, use_sessions=True):
        self._engine = node.engine
        self._node = node

        self._xmlencoder = XMLEncoder()
        self._translator = MMTTranslator(self._node, use_sessions)

    def translate(self, corpora, dest_path=None, debug=False):
        if len(corpora) == 0:
            raise IllegalArgumentException('empty corpora')

        if dest_path:
            fileutils.makedirs(dest_path, exist_ok=True)

        target_lang = self._engine.target_lang
        source_lang = self._engine.source_lang

        working_dir = self._engine.get_tempdir('evaluation')
        have_references = False

        try:
            results = []

            # Process references
            corpora_path = os.path.join(working_dir, 'corpora')
            corpora = self._xmlencoder.encode(corpora, corpora_path)

            reference = os.path.join(working_dir, 'reference.' + target_lang)
            source = os.path.join(working_dir, 'source.' + source_lang)
            refs = [corpus.get_file(target_lang) for corpus in corpora if corpus.get_file(target_lang)]
            have_references = len(refs) > 0
            fileutils.merge(refs, reference)  # tolerates missing reference
            fileutils.merge([corpus.get_file(source_lang) for corpus in corpora], source)

            if dest_path:
                for corpus in corpora:
                    corpus.copy(dest_path, suffixes={source_lang: '.src', target_lang: '.ref', 'tmx': '.src'})

            # Translate
            translator = self._translator
            name = translator.name()

            result = _EvaluationResult(translator)
            results.append(result)

            translations_path = os.path.join(working_dir, 'translations', result.id + '.raw')
            xmltranslations_path = os.path.join(working_dir, 'translations', result.id)
            fileutils.makedirs(translations_path, exist_ok=True)

            try:
                translated, mtt, parallelism = translator.translate(corpora, translations_path)
                filename = result.id + '.' + target_lang

                result.mtt = mtt
                result.parallelism = parallelism
                result.translated_corpora = self._xmlencoder.encode(translated, xmltranslations_path)
                result.merge = os.path.join(working_dir, filename)

                fileutils.merge([corpus.get_file(target_lang)
                                 for corpus in result.translated_corpora], result.merge)

                if dest_path:
                    for corpus in result.translated_corpora:
                        corpus.copy(dest_path, suffixes={target_lang: '.hyp', 'tmx': '.hyp'})

            except TranslateError as e:
                result.error = e
            except Exception as e:
                result.error = TranslateError('Unexpected ERROR: ' + str(e.message))

            if result.error is None:
                if have_references:
                    scorer = BLEUScore()
                    # bleu in range [0;1)
                    bleu = scorer.calculate(result.merge, reference)
                    return bleu
                else:
                    return True
            else:
                print(result.error)
                return None
        finally:
            if not debug:
                self._engine.clear_tempdir('evaluation')
