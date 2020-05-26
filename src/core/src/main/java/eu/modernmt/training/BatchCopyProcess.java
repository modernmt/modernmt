package eu.modernmt.training;

import eu.modernmt.io.Corpora;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by davide on 28/08/17.
 */
public class BatchCopyProcess {

    public interface OutputCorpusFactory {

        MultilingualCorpus getOutput(MultilingualCorpus corpus);

        Corpus getOutput(Corpus corpus);

    }

    private static final int MAX_IO_THREADS = 10;

    private ArrayList<MultilingualCorpus> multilingualCorpora = new ArrayList<>();
    private ArrayList<Corpus> monolingualCorpora = new ArrayList<>();

    private final OutputCorpusFactory outputFactory;

    private int ioThreads = MAX_IO_THREADS;

    public BatchCopyProcess(OutputCorpusFactory outputFactory) {
        this.outputFactory = outputFactory;
    }

    public void add(MultilingualCorpus corpus) {
        this.multilingualCorpora.add(corpus);
    }

    public void add(Corpus corpus) {
        this.monolingualCorpora.add(corpus);
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
        if (this.multilingualCorpora.isEmpty() && this.monolingualCorpora.isEmpty())
            return;

        int totalCorporaCount = this.multilingualCorpora.size() + this.monolingualCorpora.size();
        int ioThreads = Math.min(Math.min(this.ioThreads, MAX_IO_THREADS), totalCorporaCount);

        ExecutorService executor = Executors.newFixedThreadPool(ioThreads);
        List<Future<?>> futures = new ArrayList<>(totalCorporaCount);

        // Enqueue multilingual corpora tasks
        for (MultilingualCorpus corpus : multilingualCorpora) {
            final MultilingualCorpus output = outputFactory.getOutput(corpus);
            futures.add(executor.submit(() -> {
                Corpora.copy(corpus, output);
                return null;
            }));
        }

        // Enqueue monolingual corpora tasks
        for (Corpus corpus : monolingualCorpora) {
            final Corpus output = outputFactory.getOutput(corpus);
            futures.add(executor.submit(() -> {
                Corpora.copy(corpus, output);
                return null;
            }));
        }

        try {
            for (Future<?> future : futures)
                future.get();
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
