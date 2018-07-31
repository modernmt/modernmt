package eu.modernmt.context.lucene;

import eu.modernmt.context.lucene.storage.CorporaStorage;
import eu.modernmt.context.lucene.storage.Options;
import eu.modernmt.data.DataBatch;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TCorporaStorage extends CorporaStorage {

    private static Options defaultOptions() {
        Options options = new Options();
        options.enableAnalysis = false;
        return options;
    }

    private static File makeTempDirectory() throws IOException {
        File dir = Files.createTempDirectory("TCorporaStorage").toFile();
        FileUtils.forceMkdir(dir);
        return dir;
    }

    public TCorporaStorage() throws IOException {
        super(makeTempDirectory(), defaultOptions(), null);
    }

    public void onDataReceived(Collection<TranslationUnit> units) throws IOException {
        final HashMap<Short, Long> positions = new HashMap<>();
        for (TranslationUnit unit : units) {
            Long existingPosition = positions.get(unit.channel);

            if (existingPosition == null || existingPosition < unit.channelPosition)
                positions.put(unit.channel, unit.channelPosition);
        }

        super.onDataReceived(new DataBatch() {
            @Override
            public Collection<TranslationUnit> getTranslationUnits() {
                return units;
            }

            @Override
            public Collection<Deletion> getDeletions() {
                return Collections.emptyList();
            }

            @Override
            public Map<Short, Long> getChannelPositions() {
                return positions;
            }
        });
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            FileUtils.deleteQuietly(super.path);
        }
    }
}
