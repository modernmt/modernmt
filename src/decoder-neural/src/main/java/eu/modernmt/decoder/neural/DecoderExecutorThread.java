package eu.modernmt.decoder.neural;

import eu.modernmt.decoder.TranslationTimeoutException;
import eu.modernmt.decoder.neural.queue.DecoderQueue;
import eu.modernmt.decoder.neural.queue.PythonDecoder;
import eu.modernmt.decoder.neural.scheduler.Scheduler;
import eu.modernmt.decoder.neural.scheduler.TranslationSplit;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Translation;

import java.util.ArrayList;
import java.util.List;

public class DecoderExecutorThread extends Thread {

    private final Scheduler scheduler;
    private final DecoderQueue queue;
    private final DecoderExecutor executor;

    public DecoderExecutorThread(Scheduler scheduler, DecoderQueue queue, DecoderExecutor executor) {
        this.scheduler = scheduler;
        this.queue = queue;
        this.executor = executor;
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
                        executor.align(decoder, language, splits);
                    else
                        executor.translate(decoder, language, splits, job.getSuggestions());
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

}
