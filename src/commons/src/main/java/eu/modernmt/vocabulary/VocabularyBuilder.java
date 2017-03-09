package eu.modernmt.vocabulary;

import java.io.IOException;

/**
 * Created by davide on 01/09/16.
 */
public interface VocabularyBuilder {

    int add(String word) throws IOException;

    int[] addLine(String[] line) throws IOException;

    int[][] addLines(String[][] lines) throws IOException;

    Vocabulary build() throws IOException;

}
