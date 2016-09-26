package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextScore;
import eu.modernmt.context.lucene.storage.CorporaStorage;
import eu.modernmt.context.lucene.storage.Options;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.impl.StringCorpus;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
import eu.modernmt.updating.Update;
import eu.modernmt.updating.UpdatesListener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 09/05/16.
 */
public class LuceneAnalyzer implements ContextAnalyzer, UpdatesListener {

    private final ContextAnalyzerIndex index;
    private final CorporaStorage storage;

    public LuceneAnalyzer(File indexPath, Locale language) throws IOException {
        this(indexPath, language, new Options());
    }

    public LuceneAnalyzer(File indexPath, Locale language, Options options) throws IOException {
        this.index = new ContextAnalyzerIndex(new File(indexPath, "index"), language);
        this.storage = new CorporaStorage(new File(indexPath, "storage"), options, this.index);
    }

    @Override
    public void add(Domain domain, Corpus corpus) throws ContextAnalyzerException {
        HashMap<Domain, Corpus> map = new HashMap<>(1);
        map.put(domain, corpus);

        this.add(map);
    }

    @Override
    public void add(Map<Domain, Corpus> corpora) throws ContextAnalyzerException {
        for (Map.Entry<Domain, Corpus> entry : corpora.entrySet()) {
            int id = entry.getKey().getId();

            try {
                this.storage.bulkInsert(id, entry.getValue());
            } catch (IOException e) {
                throw new ContextAnalyzerException("Unable to add domain " + id, e);
            }
        }

        try {
            this.storage.flushToDisk(false, true);
        } catch (IOException e) {
            throw new ContextAnalyzerException("Unable to write storage index to disk", e);
        }
    }

    @Override
    public List<ContextScore> getContext(String query, int limit) throws ContextAnalyzerException {
        return getContext(new StringCorpus(null, null, query), limit);
    }

    @Override
    public List<ContextScore> getContext(File source, int limit) throws ContextAnalyzerException {
        return getContext(new FileCorpus(source, null, null), limit);
    }

    @Override
    public List<ContextScore> getContext(Corpus query, int limit) throws ContextAnalyzerException {
        return this.index.getSimilarDocuments(query, limit);
    }

    @Override
    public void close() throws IOException {
        try {
            this.storage.shutdown();
            this.storage.awaitTermination(TimeUnit.SECONDS, 2);
        } catch (InterruptedException e) {
            // Ignore it
        } finally {
            this.index.close();
        }
    }

    // UpdateListener

    @Override
    public void updateReceived(Update update) throws IOException, InterruptedException {
        storage.updateReceived(update);
    }

    @Override
    public Map<Integer, Long> getLatestSequentialNumbers() {
        return storage.getLatestSequentialNumbers();
    }

}
