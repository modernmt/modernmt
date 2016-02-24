package eu.modernmt.engine.training.partitioning;

import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.model.impl.BilingualFileCorpus;
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
    public BilingualCorpus getDestinationParallelCorpus(BilingualCorpus input) throws IOException {
        if (!rootDirectory.isDirectory()) {
            synchronized (this) {
                if (!rootDirectory.isDirectory())
                    FileUtils.forceMkdir(rootDirectory);
            }
        }

        return new BilingualFileCorpus(rootDirectory, input.getName(), input.getSourceLanguage(), input.getTargetLanguage());
    }
}
