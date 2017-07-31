package eu.modernmt.training;

import eu.modernmt.cleaning.Cleaner;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Created by davide on 24/02/16.
 */
public class CleaningPipeline {

    public interface OutputCorpusFactory {

        MultilingualCorpus getOutput(MultilingualCorpus corpus);

    }

    private static final int MAX_IO_THREADS = 10;

    private ArrayList<MultilingualCorpus> bilingualCorpora = new ArrayList<>();

    private final OutputCorpusFactory outputFactory;

    private int ioThreads = MAX_IO_THREADS;

    public CleaningPipeline(OutputCorpusFactory outputFactory) {
        this.outputFactory = outputFactory;
    }

    public void add(MultilingualCorpus corpus) {
        this.bilingualCorpora.add(Cleaner.wrap(corpus));
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(int ioThreads) {
        if (ioThreads < 1)
            throw new IllegalArgumentException();

        this.ioThreads = ioThreads;
    }

    public void process() throws IOException {
        int totalCorporaCount = this.bilingualCorpora.size();
        int ioThreads = Math.min(Math.min(this.ioThreads, MAX_IO_THREADS), totalCorporaCount);

        ExecutorService executor = Executors.newFixedThreadPool(ioThreads);
        ExecutorCompletionService<Void> ecs = new ExecutorCompletionService<>(executor);

        int pendingTasks = 0;

        // Enqueue bilingual corpora tasks
        for (MultilingualCorpus corpus : bilingualCorpora) {
            CleaningTask task = new CleaningTask(corpus, outputFactory.getOutput(corpus));
            ecs.submit(task);
            pendingTasks++;
        }

        try {
            for (int i = 0; i < pendingTasks; i++) {
                ecs.take().get();
            }
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof InterruptedException)
                throw new IOException("Execution interrupted", cause);
            else if (cause instanceof IOException)
                throw (IOException) cause;
            else if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new Error("Unexpected exception", cause);
        } catch (InterruptedException e) {
            throw new IOException("Execution interrupted", e);
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Nothing to do
            }
        }
    }

}
