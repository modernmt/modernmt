package eu.modernmt.engine.training;

import eu.modernmt.engine.training.partitioning.CorporaPartition;
import eu.modernmt.engine.training.partitioning.PreprocessorTask;
import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.ProcessingPipeline;
import eu.modernmt.processing.tags.TagHighlighter;
import eu.modernmt.processing.tokenizer.Tokenizers;
import eu.modernmt.processing.util.SentenceOutputter;
import eu.modernmt.processing.util.StringNormalizer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

/**
 * Created by davide on 28/01/16.
 */
public class TrainingCorporaProcessor {

    private static final int MAX_IO_THREADS = 10;
    private static final double MAX_CORPUS_PARTITION_RATIO = 0.01;

    private List<BilingualCorpus> corpora = new ArrayList<>();
    private List<CorporaPartition> extraPartitions = new ArrayList<>();
    private CorporaPartition mainPartition;
    private Locale sourceLanguage = null;
    private Locale targetLanguage = null;

    public TrainingCorporaProcessor(CorporaPartition mainPartition) {
        this.mainPartition = mainPartition;
    }

    public TrainingCorporaProcessor(CorporaPartition mainPartition, List<? extends BilingualCorpus> corpora) {
        this.mainPartition = mainPartition;
        this.corpora.addAll(corpora);
    }

    public void add(BilingualCorpus corpus) {
        this.corpora.add(corpus);
    }

    public void addAll(Collection<BilingualCorpus> corpora) {
        this.corpora.addAll(corpora);
    }

    public void addExtraPartition(CorporaPartition partition) {
        this.extraPartitions.add(partition);
    }

    public Locale getSourceLanguage() {
        if (sourceLanguage == null) {
            synchronized (this) {
                if (sourceLanguage == null && !corpora.isEmpty()) {
                    sourceLanguage = corpora.get(0).getSourceLanguage();
                }
            }
        }

        return sourceLanguage;
    }

    public Locale getTargetLanguage() {
        if (targetLanguage == null) {
            synchronized (this) {
                if (targetLanguage == null && !corpora.isEmpty()) {
                    targetLanguage = corpora.get(0).getTargetLanguage();
                }
            }
        }

        return targetLanguage;
    }

    public void process() throws ProcessingException, InterruptedException {
        this.process(corpora.size(), Runtime.getRuntime().availableProcessors());
    }

    public void process(int ioThreads, int processingThreads) throws InterruptedException, ProcessingException {
        ioThreads = Math.min(ioThreads, MAX_IO_THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(ioThreads);

        long corporaLines = getCorporaLineCount(executor);
        long extraPartitionsLines = getExtraPartitionsLines();

        // Init pipelines
        int threadsPerPipeline = Math.max(1, processingThreads / 2);
        Locale sourceLanguage = getSourceLanguage();
        Locale targetLanguage = getTargetLanguage();

        ProcessingPipeline<String, String> sourcePipeline = new ProcessingPipeline.Builder<String, String>()
                .setThreads(threadsPerPipeline)
                .add(new StringNormalizer())
                .add(Tokenizers.forLanguage(sourceLanguage))
                .add(new TagHighlighter())
                .add(new SentenceOutputter(false))
                .create();

        ProcessingPipeline<String, String> targetPipeline = new ProcessingPipeline.Builder<String, String>()
                .setThreads(threadsPerPipeline)
                .add(new StringNormalizer())
                .add(Tokenizers.forLanguage(targetLanguage))
                .add(new TagHighlighter())
                .add(new SentenceOutputter(false))
                .create();

        // Run processing
        ArrayList<Future<Void>> tasks = new ArrayList<>(corpora.size());
        for (BilingualCorpus corpus : corpora) {
            double weight = getAdjustedWeight(corpus, extraPartitionsLines, corporaLines);

            PreprocessorTask task = new PreprocessorTask(corpus, mainPartition, sourcePipeline, targetPipeline);
            for (CorporaPartition partition : extraPartitions) {
                int size = (int) Math.round(weight * partition.getSize());
                if (size > 0)
                    task.addExtraPartition(partition, size);
            }

            tasks.add(executor.submit(task));
        }

        try {
            for (Future<Void> future : tasks) {
                future.get();
            }
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof InterruptedException)
                throw (InterruptedException) cause;
            else if (cause instanceof ProcessingException)
                throw (ProcessingException) cause;
            else if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new Error("Unexpected exception", cause);
        } finally {
            IOUtils.closeQuietly(sourcePipeline);
            IOUtils.closeQuietly(targetPipeline);

            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.DAYS);
        }
    }

    private double getAdjustedWeight(BilingualCorpus corpus, long extraPartitionsLines, long corporaLines) {
        int corpusLines;
        try {
            corpusLines = corpus.getLineCount();
        } catch (IOException e) {
            throw new Error("This cannot happen", e);
        }

        double weight = ((double) corpusLines) / corporaLines;
        int expectedSize = (int) Math.round(weight * extraPartitionsLines);
        int maxAllowedLines = (int) Math.round(corpusLines * MAX_CORPUS_PARTITION_RATIO);

        if (expectedSize > maxAllowedLines) {
            return ((double) maxAllowedLines) / extraPartitionsLines;
        } else {
            return weight;
        }
    }

    private long getExtraPartitionsLines() {
        long count = 0;

        for (CorporaPartition partition : extraPartitions)
            count += partition.getSize();

        return count;
    }

    private long getCorporaLineCount(ExecutorService executor) throws ProcessingException {
        ArrayList<Future<Long>> counts = new ArrayList<>(corpora.size());
        for (BilingualCorpus corpus : corpora) {
            counts.add(executor.submit(() -> (long) corpus.getLineCount()));
        }

        long count = 0;
        for (Future<Long> c : counts) {
            try {
                count += c.get();
            } catch (InterruptedException e) {
                throw new RuntimeException("Unexpected exception", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();

                if (cause instanceof IOException)
                    throw new ProcessingException("Unable to read from corpus", e);
                else if (cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                else
                    throw new Error("Unexpected exception", cause);
            }
        }

        return count;
    }

}
