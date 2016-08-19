package eu.modernmt.training.preprocessing;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.PipelineInputStream;
import eu.modernmt.processing.framework.PipelineOutputStream;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.util.TokensOutputter;
import eu.modernmt.training.partitioning.CorporaPartition;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

/**
 * Created by davide on 24/02/16.
 */
public class TrainingPreprocessor {

    private final int threads;
    private final CorporaPartition mainPartition;
    private final Locale sourceLanguage;
    private final Locale targetLanguage;

    private ArrayList<Corpus> sourceCorpora = new ArrayList<>();
    private ArrayList<Corpus> targetCorpora = new ArrayList<>();

    public TrainingPreprocessor(CorporaPartition mainPartition, Locale source, Locale target) {
        this(mainPartition, source, target, Runtime.getRuntime().availableProcessors());
    }

    public TrainingPreprocessor(CorporaPartition mainPartition, Locale source, Locale target, int threads) {
        this.threads = threads;
        this.mainPartition = mainPartition;
        this.sourceLanguage = source;
        this.targetLanguage = target;
    }

    public void add(BilingualCorpus corpus) {
        addSourceCorpus(corpus.getSourceCorpus());
        addTargetCorpus(corpus.getTargetCorpus());
    }

    public void add(Corpus corpus) {
        addTargetCorpus(corpus);
    }

    public void addBilingualCorpora(Collection<? extends BilingualCorpus> corpora) {
        corpora.forEach(this::add);
    }

    public void addMonolingualCorpora(Collection<? extends Corpus> corpora) {
        corpora.forEach(this::add);
    }

    private void addTargetCorpus(Corpus corpus) {
        if (!targetLanguage.equals(corpus.getLanguage()))
            throw new IllegalArgumentException("Invalid corpus, expected '" + targetLanguage + "' but found '" + corpus.getLanguage() + "'");
        targetCorpora.add(corpus);
    }

    private void addSourceCorpus(Corpus corpus) {
        if (!sourceLanguage.equals(corpus.getLanguage()))
            throw new IllegalArgumentException("Invalid corpus, expected '" + sourceLanguage + "' but found '" + corpus.getLanguage() + "'");
        sourceCorpora.add(corpus);
    }

    public void execute() throws ProcessingException {
        process(sourceCorpora, sourceLanguage);
        process(targetCorpora, targetLanguage);
    }

    private void process(ArrayList<Corpus> corpora, Locale language) throws ProcessingException {
        PreprocessorExecutor executor = new PreprocessorExecutor(threads, language);

        for (Corpus corpus : corpora) {
            LineReader input = null;

            try {
                input = corpus.getContentReader();

                // Output
                Corpus outCorpus = mainPartition.getDestinationCorpus(corpus);
                LineWriter writer = outCorpus.getContentWriter(false);

                output = new TokensOutputter(writer, false, true);

                // Process
                preprocessor.process(input, output, true);
            } catch (IOException | ProcessingException e) {
                throw new ProcessingException("Failed to process corpus '" + corpus.getName() + "'", e);
            } finally {
                IOUtils.closeQuietly(input);
                IOUtils.closeQuietly(output);
            }
        }
    }
//
//    public void setVocabularyOutput(File vocabulary) {
//        vocabularyBuilder = new VocabularyBuilder(vocabulary);
//    }
//
//    public void addExtraPartition(CorporaPartition partition) {
//        this.extraPartitions.add(partition);
//    }
//
//    public void process() throws InterruptedException, ProcessingException, IOException {
//        int totalCorporaCount = this.bilingualCorpora.size() * 2 + this.monolingualCorpora.size();
//        int ioThreads = Math.min(Math.min(this.ioThreads, MAX_IO_THREADS), totalCorporaCount);
//        int processingThreads = Math.max(1, this.processingThreads);
//
//        ExecutorService executor = Executors.newFixedThreadPool(ioThreads);
//        ExecutorCompletionService<Void> ecs = new ExecutorCompletionService<>(executor);
//
//        // Init pipelines
//        Preprocessor sourcePreprocessor = new Preprocessor(sourceLanguage, null, processingThreads);
//        Preprocessor targetPreprocessor = new Preprocessor(targetLanguage, null, processingThreads);
//
//        int pendingTasks = 0;
//
//        // Enqueue bilingual corpora tasks
//        long bilingualCorporaLines = getCorporaLineCount(bilingualCorpora, ioThreads);
//        long extraPartitionsLines = getPartitionsLines(extraPartitions);
//
//        for (BilingualCorpus corpus : bilingualCorpora) {
//            double weight = getAdjustedWeight(corpus, extraPartitionsLines, bilingualCorporaLines);
//
//            int lineCount;
//            try {
//                lineCount = corpus.getLineCount();
//            } catch (IOException e) {
//                throw new ProcessingException("Could not read corpus " + corpus, e);
//            }
//
//            TrainingCorpusTask sourceTask = new TrainingCorpusTask(sourcePreprocessor, corpus.getSourceCorpus(), lineCount, mainPartition);
//            TrainingCorpusTask targetTask = new TrainingCorpusTask(targetPreprocessor, corpus.getTargetCorpus(), lineCount, mainPartition);
//
//            if (vocabularyBuilder != null) {
//                sourceTask.setVocabularyBuilder(vocabularyBuilder);
//                targetTask.setVocabularyBuilder(vocabularyBuilder);
//            }
//
//            for (CorporaPartition partition : extraPartitions) {
//                int size = (int) Math.round(weight * partition.getSize());
//                if (size > 0) {
//                    sourceTask.addExtraPartition(partition, size);
//                    targetTask.addExtraPartition(partition, size);
//                }
//            }
//
//            ecs.submit(sourceTask);
//            ecs.submit(targetTask);
//            pendingTasks += 2;
//        }
//
//        // Enqueue monolingual corpora tasks
//        for (Corpus corpus : monolingualCorpora) {
//            TrainingCorpusTask task = new TrainingCorpusTask(targetPreprocessor, corpus, 0, mainPartition);
//
//            if (vocabularyBuilder != null)
//                task.setVocabularyBuilder(vocabularyBuilder);
//
//            ecs.submit(task);
//            pendingTasks += 1;
//        }
//
//        try {
//            for (int i = 0; i < pendingTasks; i++) {
//                ecs.take().get();
//            }
//
//            if (vocabularyBuilder != null)
//                vocabularyBuilder.build();
//        } catch (ExecutionException e) {
//            Throwable cause = e.getCause();
//
//            if (cause instanceof InterruptedException)
//                throw (InterruptedException) cause;
//            else if (cause instanceof ProcessingException)
//                throw (ProcessingException) cause;
//            else if (cause instanceof RuntimeException)
//                throw (RuntimeException) cause;
//            else
//                throw new Error("Unexpected exception", cause);
//        } finally {
//            IOUtils.closeQuietly(sourcePreprocessor);
//            IOUtils.closeQuietly(targetPreprocessor);
//
//            executor.shutdownNow();
//            executor.awaitTermination(1, TimeUnit.SECONDS);
//        }
//    }
//
//    private static double getAdjustedWeight(BilingualCorpus corpus, long extraPartitionsLines, long corporaLines) {
//        int corpusLines;
//        try {
//            corpusLines = corpus.getLineCount();
//        } catch (IOException e) {
//            throw new Error("This cannot happen", e);
//        }
//
//        double weight = ((double) corpusLines) / corporaLines;
//        int expectedSize = (int) Math.round(weight * extraPartitionsLines);
//        int maxAllowedLines = (int) Math.round(corpusLines * MAX_CORPUS_PARTITION_RATIO);
//
//        if (expectedSize > maxAllowedLines) {
//            return ((double) maxAllowedLines) / extraPartitionsLines;
//        } else {
//            return weight;
//        }
//    }
//
//    private static long getPartitionsLines(Collection<CorporaPartition> partitions) {
//        long count = 0;
//
//        for (CorporaPartition partition : partitions)
//            count += partition.getSize();
//
//        return count;
//    }
//
//    private static long getCorporaLineCount(Collection<BilingualCorpus> corpora, int threads) throws ProcessingException, InterruptedException {
//        ExecutorService executor = null;
//
//        try {
//            executor = threads > 1 ? Executors.newFixedThreadPool(threads) : Executors.newSingleThreadExecutor();
//
//            ArrayList<Future<Long>> counts = new ArrayList<>(corpora.size());
//            for (BilingualCorpus corpus : corpora) {
//                counts.add(executor.submit(() -> (long) corpus.getLineCount()));
//            }
//
//            long count = 0;
//            for (Future<Long> c : counts) {
//                try {
//                    count += c.get();
//                } catch (ExecutionException e) {
//                    Throwable cause = e.getCause();
//
//                    if (cause instanceof IOException)
//                        throw new ProcessingException("Unable to read from corpus", e);
//                    else if (cause instanceof RuntimeException)
//                        throw (RuntimeException) cause;
//                    else
//                        throw new Error("Unexpected exception", cause);
//                }
//            }
//
//            return count;
//        } finally {
//            if (executor != null) {
//                executor.shutdownNow();
//                executor.awaitTermination(1, TimeUnit.SECONDS);
//            }
//        }
//    }

}
