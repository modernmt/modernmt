package eu.modernmt.decoder.neural;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.queue.PythonDecoder;
import eu.modernmt.decoder.neural.scheduler.TranslationSplit;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.util.Collection;
import java.util.List;

public class DecoderExecutorImpl implements DecoderExecutor {

    @Override
    public void align(PythonDecoder decoder, LanguageDirection language, List<TranslationSplit> splits) throws DecoderException {
        Sentence[] sentences = mergeSentences(splits);
        String[][] references = mergeReferences(splits);

        Translation[] result = decoder.align(language, sentences, references);

        int i = 0;
        for (TranslationSplit split : splits)
            split.setTranslation(result[i++]);
    }

    @Override
    public void translate(PythonDecoder decoder, LanguageDirection language, List<TranslationSplit> splits, Collection<ScoreEntry> suggestions, List<Integer> alternatives) throws DecoderException {
        Sentence[] sentences = mergeSentences(splits);
        Translation[] translations;
        Integer[] alternativesArray = alternatives != null && alternatives.size() > 0 ? alternatives.toArray(new Integer[0]) : null;
        if (suggestions == null || suggestions.isEmpty()) {
            translations = decoder.translate(language, sentences, alternativesArray);
        } else {
            ScoreEntry[] suggestionArray = suggestions.toArray(new ScoreEntry[0]);
            translations = decoder.translate(language, sentences, suggestionArray, alternativesArray);
        }

        int i = 0;
        for (TranslationSplit split : splits)
            split.setTranslation(translations[i++]);
    }

    private static Sentence[] mergeSentences(List<TranslationSplit> splits) {
        Sentence[] sentences = new Sentence[splits.size()];
        for (int i = 0; i < sentences.length; i++)
            sentences[i] = splits.get(i).sentence;
        return sentences;
    }

    private static String[][] mergeReferences(List<TranslationSplit> splits) {
        String[][] result = new String[splits.size()][];

        int i = 0;
        for (TranslationSplit split : splits)
            result[i++] = split.reference;

        return result;
    }

}
