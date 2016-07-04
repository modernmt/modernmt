package eu.modernmt.core.training;

import eu.modernmt.model.BilingualCorpus;
import org.apache.commons.io.IOUtils;

import java.util.concurrent.Callable;

/**
 * Created by davide on 24/02/16.
 */
class CleaningCorpusTask implements Callable<Void> {

    private BilingualCorpus corpus;
    private BilingualCorpus output;

    public CleaningCorpusTask(BilingualCorpus corpus, BilingualCorpus output) {
        this.corpus = corpus;
        this.output = output;
    }

    @Override
    public Void call() throws Exception {
        BilingualCorpus.BilingualLineReader reader = null;
        BilingualCorpus.BilingualLineWriter writer = null;

        try {
            reader = corpus.getContentReader();
            writer = output.getContentWriter(false);

            BilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                writer.write(pair);
            }
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }

        return null;
    }
}
