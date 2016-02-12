package eu.modernmt.engine.training.preprocessing;

import eu.modernmt.model.ParallelCorpus;
import eu.modernmt.model.ParallelFilesCorpus;
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
    public ParallelCorpus getDestinationParallelCorpus(ParallelCorpus input) throws IOException {
        if (!rootDirectory.isDirectory()) {
            synchronized (this) {
                if (!rootDirectory.isDirectory())
                    FileUtils.forceMkdir(rootDirectory);
            }
        }

        return new ParallelFilesCorpus(rootDirectory, input.getName(), input.getSourceLanguage(), input.getTargetLanguage());
    }
}
