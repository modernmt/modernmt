package eu.modernmt.core.engine;

import eu.modernmt.contextanalyzer.ContextAnalyzer;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.moses.MosesDecoder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by davide on 27/11/15.
 */
public class TranslationEngine {

    private static String path(String... path) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < path.length; i++) {
            result.append(path[i]);
            if (i < path.length - 1)
                result.append(File.pathSeparatorChar);
        }

        return result.toString();
    }

    public static final String CONTEXT_ANALYZER_INDEX_PATH = path("data", "context", "index");
    public static final String MOSES_INI_PATH = path("data", "moses.ini");

    private File root;

    private ContextAnalyzer contextAnalyzer;
    private MosesDecoder decoder;

    public TranslationEngine(File engines, String name) throws IOException {
        this(new File(engines, name));
    }

    public TranslationEngine(File root) throws IOException {
        if (!root.isDirectory())
            throw new FileNotFoundException(root.toString());

        this.root = root;
    }

    public ContextAnalyzer getContextAnalyzer() throws IOException {
        if (contextAnalyzer == null) {
            File index = new File(this.root, CONTEXT_ANALYZER_INDEX_PATH);
            contextAnalyzer = new ContextAnalyzer(index);
        }

        return contextAnalyzer;
    }

    public Decoder getDecoder() {
        if (decoder == null) {
            File mosesIni = new File(this.root, MOSES_INI_PATH);
            decoder = new MosesDecoder(mosesIni);
        }

        return decoder;
    }

}
