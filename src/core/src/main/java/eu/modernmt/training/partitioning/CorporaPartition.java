package eu.modernmt.training.partitioning;

import eu.modernmt.lang.Language;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 11/02/16.
 */
public class CorporaPartition {

    private final File rootDirectory;
    private final int size;

    public CorporaPartition(File directory) {
        this(directory, 0);
    }

    public CorporaPartition(File directory, int size) {
        this.rootDirectory = directory;
        this.size = size;
    }

    public final int getSize() {
        return size;
    }

    public Corpus getDestinationCorpus(Corpus sourceCorpus) throws IOException {
        if (!rootDirectory.isDirectory()) {
            synchronized (this) {
                if (!rootDirectory.isDirectory())
                    FileUtils.forceMkdir(rootDirectory);
            }
        }

        Language language = sourceCorpus.getLanguage();
        String name = sourceCorpus.getName();
        String filename = name + "." + language.toLanguageTag();

        return new FileCorpus(new File(rootDirectory, filename), name, language);
    }

}
