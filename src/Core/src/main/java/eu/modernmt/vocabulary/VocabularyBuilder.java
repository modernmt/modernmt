package eu.modernmt.vocabulary;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 16/08/16.
 */
public class VocabularyBuilder {

    private final HashMap<String, Integer> vocabulary;
    private final File model;

    private int idCounter = Vocabulary.VOCABULARY_WORD_ID_START;

    public VocabularyBuilder(File model) {
        this(model, 1000000);
    }

    public VocabularyBuilder(File model, int initialCapacity) {
        this.model = model;
        this.vocabulary = new HashMap<>(initialCapacity);
    }

    public synchronized int add(String word) {
        Integer id = vocabulary.get(word);

        if (id == null) {
            id = idCounter++;
            vocabulary.put(word, id);
        }

        return id;
    }

    public synchronized int[] addLine(String[] line) {
        int[] result = new int[line.length];

        for (int i = 0; i < line.length; i++) {
            String word = line[i];
            Integer id = vocabulary.get(word);

            if (id == null) {
                id = idCounter++;
                vocabulary.put(word, id);
            }

            result[i] = id;
        }

        return result;
    }

    public synchronized int[][] addLines(String[][] lines) {
        int[][] result = new int[lines.length][];

        for (int i = 0; i < lines.length; i++) {
            result[i] = addLine(lines[i]);
        }

        return result;
    }

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

        return new Vocabulary(model);
    }

    private native void flush(String[] words, int[] ids, int id, String path);

}
