class CompactCorpus(object):
    def __init__(self, path):
        self.path = path

    @staticmethod
    def _lang_match(lang, target):
        if lang == target:
            return True

        lang = lang.split('-')[0]

        return lang == target

    def reader(self):
        class __r:
            def __init__(self, path):
                self._path = path

            def __enter__(self):
                self._file = open(self._path, 'rb')
                return self

            def __exit__(self, exc_type, exc_val, exc_tb):
                self._file.close()

            def __iter__(self):
                return self

            def next(self):
                sentence = self._readline()

                if not sentence:
                    raise StopIteration

                translation = self._readline()
                metadata = self._readline()

                source, target = metadata.split(',')[1].split()

                return source, target, sentence, translation

            def _readline(self):
                return self._file.readline().strip().decode('utf-8')

        return __r(self.path)

    def readall(self, source, target):
        source_lines = []
        target_lines = []

        with self.reader() as reader:
            for s, t, sentence, translation in reader:
                if self._lang_match(s, source) and self._lang_match(t, target):
                    source_lines.append(sentence)
                    target_lines.append(translation)
                elif self._lang_match(t, source) and self._lang_match(s, target):
                    target_lines.append(sentence)
                    source_lines.append(translation)

        return source_lines, target_lines
