package eu.modernmt.core.training;

import eu.modernmt.core.training.partitioning.CorporaPartition;
import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.model.Corpus;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.ProcessingPipeline;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.*;

/**
 * Created by davide on 24/02/16.
 */
public class TrainingPipeline {

    private static final int MAX_IO_THREADS = 10;
    private static final double MAX_CORPUS_PARTITION_RATIO = 0.01;

    private CorporaPartition mainPartition;
    private ArrayList<CorporaPartition> extraPartitions = new ArrayList<>();

    private ArrayList<BilingualCorpus> bilingualCorpora = new ArrayList<>();
    private ArrayList<Corpus> monolingualCorpora = new ArrayList<>();

    private final Locale sourceLanguage;
    private final Locale targetLanguage;

    private int ioThreads = MAX_IO_THREADS;
    private int processingThreads = Runtime.getRuntime().availableProcessors();

    public TrainingPipeline(CorporaPartition mainPartition, Locale source, Locale target) {
        this.mainPartition = mainPartition;
        this.sourceLanguage = source;
        this.targetLanguage = target;
    }

    public void add(BilingualCorpus corpus) {
        this.bilingualCorpora.add(corpus);
    }

    public void add(Corpus corpus) {
        this.monolingualCorpora.add(corpus);
    }

    public void addBilingualCorpora(Collection<? extends BilingualCorpus> corpora) {
        this.bilingualCorpora.addAll(corpora);
    }

    public void addMonolingualCorpora(Collection<? extends Corpus> corpora) {
        this.monolingualCorpora.addAll(corpora);
    }

    public void addExtraPartition(CorporaPartition partition) {
        this.extraPartitions.add(partition);
    }

    public Locale getSourceLanguage() {
        return sourceLanguage;
    }

    public Locale getTargetLanguage() {
        return targetLanguage;
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(int ioThreads) {
        if (ioThreads < 1)
            throw new IllegalArgumentException();

        this.ioThreads = ioThreads;
    }

    public int getProcessingThreads() {
        return processingThreads;
    }

    public void setProcessingThreads(int processingThreads) {
        if (processingThreads < 1)
            throw new IllegalArgumentException();

        this.processingThreads = processingThreads;
    }

    public void process() throws InterruptedException, ProcessingException {
        int totalCorporaCount = this.bilingualCorpora.size() * 2 + this.monolingualCorpora.size();
        int ioThreads = Math.min(Math.min(this.ioThreads, MAX_IO_THREADS), totalCorporaCount);
        int processingThreads = Math.max(1, this.processingThreads / 2);

        ExecutorService executor = Executors.newFixedThreadPool(ioThreads);
        ExecutorCompletionService<Void> ecs = new ExecutorCompletionService<>(executor);

        // Init pipelines
        ProcessingPipeline<String, Sentence> sourcePipeline = Preprocessor.getPipeline(sourceLanguage, true, processingThreads);
        ProcessingPipeline<String, Sentence> targetPipeline = Preprocessor.getPipeline(targetLanguage, true, processingThreads);

        int pendingTasks = 0;

        // Enqueue bilingual corpora tasks
        long bilingualCorporaLines = getCorporaLineCount(bilingualCorpora, ioThreads);
        long extraPartitionsLines = getPartitionsLines(extraPartitions);

        for (BilingualCorpus corpus : bilingualCorpora) {
            double weight = getAdjustedWeight(corpus, extraPartitionsLines, bilingualCorporaLines);

            int lineCount;
            try {
                lineCount = corpus.getLineCount();
            } catch (IOException e) {
                throw new ProcessingException("Could not read corpus " + corpus, e);
            }

            TrainingCorpusTask sourceTask = new TrainingCorpusTask(sourcePipeline, corpus.getSourceCorpus(), lineCount, mainPartition);
            TrainingCorpusTask targetTask = new TrainingCorpusTask(targetPipeline, corpus.getTargetCorpus(), lineCount, mainPartition);

            for (CorporaPartition partition : extraPartitions) {
                int size = (int) Math.round(weight * partition.getSize());
                if (size > 0) {
                    sourceTask.addExtraPartition(partition, size);
                    targetTask.addExtraPartition(partition, size);
                }
            }

            ecs.submit(sourceTask);
            ecs.submit(targetTask);
            pendingTasks += 2;
        }

        // Enqueue monolingual corpora tasks
        for (Corpus corpus : monolingualCorpora) {
            TrainingCorpusTask task = new TrainingCorpusTask(targetPipeline, corpus, 0, mainPartition);
            ecs.submit(task);
            pendingTasks += 1;
        }

        try {
            for (int i = 0; i < pendingTasks; i++) {
                ecs.take().get();
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
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    private static double getAdjustedWeight(BilingualCorpus corpus, long extraPartitionsLines, long corporaLines) {
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

    private static long getPartitionsLines(Collection<CorporaPartition> partitions) {
        long count = 0;

        for (CorporaPartition partition : partitions)
            count += partition.getSize();

        return count;
    }

    private static long getCorporaLineCount(Collection<BilingualCorpus> corpora, int threads) throws ProcessingException, InterruptedException {
        ExecutorService executor = null;

        try {
            executor = threads > 1 ? Executors.newFixedThreadPool(threads) : Executors.newSingleThreadExecutor();

            ArrayList<Future<Long>> counts = new ArrayList<>(corpora.size());
            for (BilingualCorpus corpus : corpora) {
                counts.add(executor.submit(() -> (long) corpus.getLineCount()));
            }

            long count = 0;
            for (Future<Long> c : counts) {
                try {
                    count += c.get();
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
        } finally {
            if (executor != null) {
                executor.shutdownNow();
                executor.awaitTermination(1, TimeUnit.SECONDS);
            }
        }
    }
}
