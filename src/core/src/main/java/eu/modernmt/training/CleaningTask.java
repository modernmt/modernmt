package eu.modernmt.training;

import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.commons.io.IOUtils;

import java.util.concurrent.Callable;

/**
 * Created by davide on 24/02/16.
 */
class CleaningTask implements Callable<Void> {

    private MultilingualCorpus corpus;
    private MultilingualCorpus output;

    public CleaningTask(MultilingualCorpus corpus, MultilingualCorpus output) {
        this.corpus = corpus;
        this.output = output;
    }

    @Override
    public Void call() throws Exception {
        MultilingualCorpus.MultilingualLineReader reader = null;
        MultilingualCorpus.MultilingualLineWriter writer = null;

        try {
            reader = corpus.getContentReader();
            writer = output.getContentWriter(false);

            MultilingualCorpus.StringPair pair;
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
