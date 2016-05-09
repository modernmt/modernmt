package eu.modernmt.processing.tokenizer.paoding;

import eu.modernmt.constants.Const;
import eu.modernmt.processing.Languages;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.util.LuceneTokenizerAdapter;
import net.paoding.analysis.analyzer.PaodingAnalyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Created by davide on 27/01/16.
 */
public class PaodingTokenizer extends LuceneTokenizerAdapter {

    public static final PaodingTokenizer CHINESE = new PaodingTokenizer();
    public static final Map<Locale, Tokenizer> ALL = new HashMap<>();

    static {
        ALL.put(Languages.CHINESE, CHINESE);
    }

    private static String PROPERTIES_FILE_PATH = null;

    private static String getPropertiesPath() {
        if (PROPERTIES_FILE_PATH == null) {
            synchronized (PaodingTokenizer.class) {
                if (PROPERTIES_FILE_PATH == null) {
                    File paoding = new File(Const.fs.tokenizerModels, "paoding");
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

                    PROPERTIES_FILE_PATH = propertiesFile.getAbsolutePath();
                }
            }
        }

        return PROPERTIES_FILE_PATH;
    }

    protected static class PaodingTokenizerFactory extends AnalyzerTokenizerFactory {

        public PaodingTokenizerFactory() {
            super(PaodingAnalyzer.class);
        }

        @Override
        public Tokenizer newInstance() {
            return new LuceneTokenizerImplementation(new PaodingAnalyzer(getPropertiesPath()));
        }
    }

    public PaodingTokenizer() {
        super(new PaodingTokenizerFactory());
    }
}
