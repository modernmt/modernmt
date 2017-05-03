package eu.modernmt.vocabulary.rocksdb;

import eu.modernmt.vocabulary.Vocabulary;
import eu.modernmt.vocabulary.VocabularyBuilder;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 01/09/16.
 */
public class RocksDBVocabularyBuilder implements VocabularyBuilder {

    private final HashMap<String, Integer> vocabulary;
    private final File model;

    private int idCounter = Vocabulary.VOCABULARY_WORD_ID_START;

    RocksDBVocabularyBuilder(File model) {
        this(model, 1000000);
    }

    RocksDBVocabularyBuilder(File model, int initialCapacity) {
        this.model = model;
        this.vocabulary = new HashMap<>(initialCapacity);
    }

    @Override
    public synchronized int add(String word) {
        return vocabulary.computeIfAbsent(word, k -> idCounter++);
    }

    @Override
    public synchronized int[] addLine(String[] line) {
        int[] result = new int[line.length];

        for (int i = 0; i < line.length; i++) {
            String word = line[i];
            result[i] = vocabulary.computeIfAbsent(word, k -> idCounter++);
        }

        return result;
    }

    @Override
    public synchronized int[][] addLines(String[][] lines) {
        int[][] result = new int[lines.length][];

        for (int i = 0; i < lines.length; i++) {
            result[i] = addLine(lines[i]);
        }

        return result;
    }

    @Override
    public synchronized Vocabulary build() throws IOException {
        if (!model.isDirectory())
            FileUtils.forceMkdir(model);

        String[] words = new String[vocabulary.size()];
        int[] ids = new int[vocabulary.size()];

        int i = 0;
        for (Map.Entry<String, Integer> entry : vocabulary.entrySet()) {
            words[i] = entry.getKey();
            ids[i] = entry.getValue();
            i++;
        }

        vocabulary.clear();

        flush(words, ids, idCounter, model.getAbsolutePath());

        return new RocksDBVocabulary(model);
    }

    private native void flush(String[] words, int[] ids, int id, String path);

}
