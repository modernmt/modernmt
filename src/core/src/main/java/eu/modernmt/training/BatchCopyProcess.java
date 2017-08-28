package eu.modernmt.training;

import eu.modernmt.io.IOCorporaUtils;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

/**
 * Created by davide on 28/08/17.
 */
public class BatchCopyProcess {

    public interface OutputCorpusFactory {

        MultilingualCorpus getOutput(MultilingualCorpus corpus);

    }

    private static final int MAX_IO_THREADS = 10;

    private ArrayList<MultilingualCorpus> corpora = new ArrayList<>();

    private final OutputCorpusFactory outputFactory;

    private int ioThreads = MAX_IO_THREADS;

    public BatchCopyProcess(OutputCorpusFactory outputFactory) {
        this.outputFactory = outputFactory;
    }

    public void add(MultilingualCorpus corpus) {
        this.corpora.add(corpus);
    }

    public void addAll(Collection<? extends MultilingualCorpus> corpora) {
        this.corpora.addAll(corpora);
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(int ioThreads) {
        if (ioThreads < 1)
            throw new IllegalArgumentException();

        this.ioThreads = ioThreads;
    }

    public void run() throws IOException {
        int totalCorporaCount = this.corpora.size();
        int ioThreads = Math.min(Math.min(this.ioThreads, MAX_IO_THREADS), totalCorporaCount);

        ExecutorService executor = Executors.newFixedThreadPool(ioThreads);
        Future<?>[] futures = new Future[totalCorporaCount];

        // Enqueue bilingual corpora tasks
        for (int i = 0; i < totalCorporaCount; i++) {
            final MultilingualCorpus corpus = corpora.get(i);
            final MultilingualCorpus output = outputFactory.getOutput(corpus);

            futures[i] = executor.submit((Callable<Void>) () -> {
                IOCorporaUtils.copy(corpus, output);
                return null;
            });
        }

        try {
            for (Future<?> future : futures) future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof IOException)
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
