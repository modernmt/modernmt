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
public class FilesCorporaPartition extends CorporaPartition {

    private File rootDirectory;

    public FilesCorporaPartition(File directory, int size) {
        super(size);
        this.rootDirectory = directory;
    }

    public FilesCorporaPartition(File directory) {
        super();
        this.rootDirectory = directory;
    }

    @Override
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
