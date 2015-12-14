package eu.modernmt.tokenizer.paoding;

import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;
import eu.modernmt.tokenizer.Languages;
import eu.modernmt.tokenizer.lucene.LuceneTokenizer;
import net.paoding.analysis.analyzer.PaodingAnalyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Created by davide on 13/11/15.
 */
public class PaodingTokenizer extends LuceneTokenizer {

    public static final ITokenizerFactory CHINESE = new ITokenizerFactory() {
        @Override
        protected ITokenizer newInstance() {
            return new PaodingTokenizer();
        }
    };

    public static final Map<Locale, ITokenizerFactory> ALL = new HashMap<>();

    static {
        ALL.put(Languages.CHINESE, CHINESE);
    }

    private static String getPropertiesPath() {
        File paoding = new File(ITokenizer.MODELS_PATH, "paoding");
        File dic = new File(paoding, "dic");

        File propertiesFile = new File(paoding, "paoding-analysis.properties");

        Properties properties = new Properties();
        properties.setProperty("paoding.dic.home", dic.getAbsolutePath());
        properties.setProperty("paoding.knife.class.letterKnife", "net.paoding.analysis.knife.LetterKnife");
        properties.setProperty("paoding.knife.class.numberKnife", "net.paoding.analysis.knife.NumberKnife");
        properties.setProperty("paoding.knife.class.cjkKnife", "net.paoding.analysis.knife.CJKKnife");

        FileOutputStream stream = null;
        try {
            if (propertiesFile.exists() && !propertiesFile.delete())
                throw new IOException("Unable to delete file " + propertiesFile);

            if (!propertiesFile.createNewFile())
                throw new IOException("Unable to newInstance file " + propertiesFile);

            stream = new FileOutputStream(propertiesFile);
            properties.store(stream, null);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write file " + propertiesFile, e);
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                }
        }

        return propertiesFile.getAbsolutePath();
    }

    private static synchronized PaodingAnalyzer newInstance() {
        String path = getPropertiesPath();
        return new PaodingAnalyzer(path);
    }

    public PaodingTokenizer() {
        super(newInstance());
    }
}
