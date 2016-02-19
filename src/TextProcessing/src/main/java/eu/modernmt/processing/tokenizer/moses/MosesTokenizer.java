package eu.modernmt.processing.tokenizer.moses;

import eu.modernmt.processing.AnnotatedString;
import eu.modernmt.processing.Languages;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.UnixLineReader;
import eu.modernmt.processing.tokenizer.MultiInstanceTokenizer;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;
import eu.modernmt.processing.tokenizer.util.Environment;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 27/01/16.
 */
public class MosesTokenizer extends MultiInstanceTokenizer {

    public static final MosesTokenizer CATALAN = new MosesTokenizer("ca");
    public static final MosesTokenizer CZECH = new MosesTokenizer("cs");
    public static final MosesTokenizer GERMAN = new MosesTokenizer("de");
    public static final MosesTokenizer GREEK = new MosesTokenizer("el");
    public static final MosesTokenizer ENGLISH = new MosesTokenizer("en");
    public static final MosesTokenizer SPANISH = new MosesTokenizer("es");
    public static final MosesTokenizer FINNISH = new MosesTokenizer("fi");
    public static final MosesTokenizer FRENCH = new MosesTokenizer("fr");
    public static final MosesTokenizer HUNGARIAN = new MosesTokenizer("hu");
    public static final MosesTokenizer ICELANDIC = new MosesTokenizer("is");
    public static final MosesTokenizer ITALIAN = new MosesTokenizer("it");
    public static final MosesTokenizer LATVIAN = new MosesTokenizer("lv");
    public static final MosesTokenizer DUTCH = new MosesTokenizer("nl");
    public static final MosesTokenizer POLISH = new MosesTokenizer("pl");
    public static final MosesTokenizer PORTUGUESE = new MosesTokenizer("pt");
    public static final MosesTokenizer ROMANIAN = new MosesTokenizer("ro");
    public static final MosesTokenizer RUSSIAN = new MosesTokenizer("ru");
    public static final MosesTokenizer SLOVAK = new MosesTokenizer("sk");
    public static final MosesTokenizer SLOVENE = new MosesTokenizer("sl");
    public static final MosesTokenizer SWEDISH = new MosesTokenizer("sv");
    public static final MosesTokenizer TAMIL = new MosesTokenizer("ta");

    public static final Map<Locale, Tokenizer> ALL = new HashMap<>();

    static {
        ALL.put(Languages.CATALAN, CATALAN);
        ALL.put(Languages.CZECH, CZECH);
        ALL.put(Languages.GERMAN, GERMAN);
        ALL.put(Languages.GREEK, GREEK);
        ALL.put(Languages.ENGLISH, ENGLISH);
        ALL.put(Languages.SPANISH, SPANISH);
        ALL.put(Languages.FINNISH, FINNISH);
        ALL.put(Languages.FRENCH, FRENCH);
        ALL.put(Languages.HUNGARIAN, HUNGARIAN);
        ALL.put(Languages.ICELANDIC, ICELANDIC);
        ALL.put(Languages.ITALIAN, ITALIAN);
        ALL.put(Languages.LATVIAN, LATVIAN);
        ALL.put(Languages.DUTCH, DUTCH);
        ALL.put(Languages.POLISH, POLISH);
        ALL.put(Languages.PORTUGUESE, PORTUGUESE);
        ALL.put(Languages.ROMANIAN, ROMANIAN);
        ALL.put(Languages.RUSSIAN, RUSSIAN);
        ALL.put(Languages.SLOVAK, SLOVAK);
        ALL.put(Languages.SLOVENE, SLOVENE);
        ALL.put(Languages.SWEDISH, SWEDISH);
        ALL.put(Languages.TAMIL, TAMIL);
    }

    private static class MosesTokenizerFactory implements TokenizerFactory {

        private String languageCode;

        public MosesTokenizerFactory(String languageCode) {
            this.languageCode = languageCode;
        }

        @Override
        public Tokenizer newInstance() {
            return new MosesTokenizerImplementation(languageCode);
        }
    }

    public MosesTokenizer(String languageCode) {
        super(new MosesTokenizerFactory(languageCode));
    }

    private static class MosesTokenizerImplementation implements Tokenizer, AutoCloseable {

        private Process tokenizer = null;
        private OutputStream tokenizerStdin;
        private UnixLineReader tokenizerStdout;

        public MosesTokenizerImplementation(String languageCode) {
            File moses = new File(Environment.MODELS_PATH, "moses");
            File scripts = new File(moses, "scripts");
            File tokenizerScript = new File(scripts, "tokenizer.perl");
            String tokenizerCommand = "perl " + tokenizerScript.getAbsolutePath() + " -b -X -l " + languageCode + " -no-escape";

            Runtime runtime = Runtime.getRuntime();

            try {
                this.tokenizer = runtime.exec(tokenizerCommand);
                this.tokenizerStdin = tokenizer.getOutputStream();
                this.tokenizerStdout = new UnixLineReader(new InputStreamReader(tokenizer.getInputStream(), "UTF-8"));
            } catch (IOException e) {
                this.close();
                throw new RuntimeException("Error while executing processes", e);
            }
        }

        @Override
        public AnnotatedString call(String text) throws ProcessingException {
            String tokenized;

            try {
                this.tokenizerStdin.write(text.getBytes("utf-8"));
                this.tokenizerStdin.write('\n');
                this.tokenizerStdin.flush();

                tokenized = this.tokenizerStdout.readLine();
            } catch (IOException e) {
                throw new ProcessingException("Error while running perl script", e);
            }

            return new AnnotatedString(text, TokenizerOutputTransformer.transform(text, tokenized.split("\\s+")));
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(tokenizerStdin);
            IOUtils.closeQuietly(tokenizerStdout);

            if (tokenizer != null)
                tokenizer.destroy();
        }

    }
}
