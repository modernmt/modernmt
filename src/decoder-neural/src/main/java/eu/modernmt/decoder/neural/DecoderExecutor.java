package eu.modernmt.decoder.neural;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.TranslationTimeoutException;
import eu.modernmt.decoder.neural.queue.DecoderQueue;
import eu.modernmt.decoder.neural.queue.PythonDecoder;
import eu.modernmt.decoder.neural.scheduler.Scheduler;
import eu.modernmt.decoder.neural.scheduler.TranslationSplit;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.util.*;

public class DecoderExecutor extends Thread {

    private final Scheduler scheduler;
    private final DecoderQueue queue;

    public DecoderExecutor(Scheduler scheduler, DecoderQueue queue) {
        this.scheduler = scheduler;
        this.queue = queue;
    }

    private Scheduler.Job take() {
        try {
            return this.scheduler.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public void run() {
        Scheduler.Job job;

        while ((job = take()) != null) {
            List<TranslationSplit> splits = filter(job.getTranslationSplits());

            if (!splits.isEmpty()) {
                PythonDecoder decoder = null;

                try {
                    decoder = queue.take(job.getLanguageDirection());

                    long timestamp = System.currentTimeMillis();
                    for (TranslationSplit split : splits)
                        split.onTranslationBegin(timestamp);

                    LanguageDirection language = job.getLanguageDirection();
                    if (job.isAlignmentJob())
                        align(decoder, language, splits);
                    else
                        translate(decoder, language, splits, job.getSuggestions());

                    timestamp = System.currentTimeMillis();
                    for (TranslationSplit split : splits)
                        split.onTranslationEnd(timestamp);
                } catch (Throwable e) {
                    for (TranslationSplit split : job.getTranslationSplits())
                        split.setException(e);
                } finally {
                    if (decoder != null)
                        queue.release(decoder);
                }
            }
        }
    }

    private static List<TranslationSplit> filter(List<TranslationSplit> splits) {
        ArrayList<TranslationSplit> result = new ArrayList<>(splits.size());

        for (TranslationSplit split : splits) {
            try {
                split.ensureValid();

                if (split.sentence.hasWords())
                    result.add(split);
                else
                    split.setTranslation(Translation.emptyTranslation(split.sentence));
            } catch (TranslationTimeoutException e) {
                split.setException(e);
            }
        }

        return result;
    }

    private static void align(PythonDecoder decoder, LanguageDirection language, List<TranslationSplit> splits) throws DecoderException {
        Sentence[] sentences = mergeSentences(splits);
        String[][] references = mergeReferences(splits);

        Translation[] result = decoder.align(language, sentences, references);

        int i = 0;
        for (TranslationSplit split : splits)
            split.setTranslation(result[i++]);
    }

    private static void translate(PythonDecoder decoder, LanguageDirection language, List<TranslationSplit> splits, Collection<ScoreEntry> suggestions) throws DecoderException {
        Sentence[] sentences = mergeSentences(splits);
        Translation[] translations;

        if (suggestions == null || suggestions.isEmpty()) {
            translations = decoder.translate(language, sentences, 0);
        } else {
            ScoreEntry[] suggestionArray = suggestions.toArray(new ScoreEntry[0]);
            translations = decoder.translate(language, sentences, suggestionArray, 0);
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
