package eu.modernmt.processing.tokenizer.opennlp;

import eu.modernmt.config.Config;
import eu.modernmt.processing.Languages;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.string.ProcessedString;
import eu.modernmt.processing.tokenizer.MultiInstanceTokenizer;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 27/01/16.
 */
public class OpenNLPTokenizer extends MultiInstanceTokenizer {

    public static final OpenNLPTokenizer DANISH = new OpenNLPTokenizer("da");
    public static final OpenNLPTokenizer GERMAN = new OpenNLPTokenizer("de");
    public static final OpenNLPTokenizer ENGLISH = new OpenNLPTokenizer("en");
    public static final OpenNLPTokenizer ITALIAN = new OpenNLPTokenizer("it");
    public static final OpenNLPTokenizer DUTCH = new OpenNLPTokenizer("nl");
    public static final OpenNLPTokenizer PORTUGUESE = new OpenNLPTokenizer("pt");
    public static final OpenNLPTokenizer NORTHERN_SAMI = new OpenNLPTokenizer("se");

    public static final Map<Locale, Tokenizer> ALL = new HashMap<>();

    static {
        ALL.put(Languages.DANISH, DANISH);
        ALL.put(Languages.GERMAN, GERMAN);
        ALL.put(Languages.ENGLISH, ENGLISH);
        ALL.put(Languages.ITALIAN, ITALIAN);
        ALL.put(Languages.DUTCH, DUTCH);
        ALL.put(Languages.PORTUGUESE, PORTUGUESE);
        ALL.put(Languages.NORTHERN_SAMI, NORTHERN_SAMI);
    }

    private static class OpenNLPTokenizerFactory implements TokenizerFactory {

        private String languageCode;

        public OpenNLPTokenizerFactory(String languageCode) {
            this.languageCode = languageCode;
        }

        @Override
        public Tokenizer newInstance() {
            File opennlp = new File(Config.fs.tokenizerModels, "opennlp");

            File modelFile = new File(opennlp, this.languageCode + "-token.bin");

            if (!modelFile.isFile())
                throw new IllegalArgumentException("Unsupported language: " + this.languageCode);

            InputStream modelResource = null;

            try {
                modelResource = new FileInputStream(modelFile);
                TokenizerModel model = new TokenizerModel(modelResource);
                return new OpenNLPTokenizerImplementation(model);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load OpenNLP model at " + modelFile, e);
            } finally {
                if (modelResource != null)
                    try {
                        modelResource.close();
                    } catch (IOException e) {
                    }
            }
        }
    }

    public OpenNLPTokenizer(String languageCode) {
        super(new OpenNLPTokenizerFactory(languageCode));
    }

    private static class OpenNLPTokenizerImplementation implements Tokenizer {

        private TokenizerME tokenizer;

        public OpenNLPTokenizerImplementation(TokenizerModel model) {
            this.tokenizer = new TokenizerME(model);
        }

        @Override
        public ProcessedString call(ProcessedString text) throws ProcessingException {
            return TokenizerOutputTransformer.transform(text, this.tokenizer.tokenize(text.toString()));
        }

        @Override
        public void close() throws IOException {

        }
    }

}
