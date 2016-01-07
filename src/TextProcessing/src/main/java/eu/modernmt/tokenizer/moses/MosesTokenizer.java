package eu.modernmt.tokenizer.moses;

import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;
import eu.modernmt.tokenizer.Languages;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by davide on 23/11/15.
 */
public class MosesTokenizer extends ITokenizer implements Closeable {

    public static final ITokenizerFactory CATALAN = new MosesTokenizerFactory("ca");
    public static final ITokenizerFactory CZECH = new MosesTokenizerFactory("cs");
    public static final ITokenizerFactory GERMAN = new MosesTokenizerFactory("de");
    public static final ITokenizerFactory GREEK = new MosesTokenizerFactory("el");
    public static final ITokenizerFactory ENGLISH = new MosesTokenizerFactory("en");
    public static final ITokenizerFactory SPANISH = new MosesTokenizerFactory("es");
    public static final ITokenizerFactory FINNISH = new MosesTokenizerFactory("fi");
    public static final ITokenizerFactory FRENCH = new MosesTokenizerFactory("fr");
    public static final ITokenizerFactory HUNGARIAN = new MosesTokenizerFactory("hu");
    public static final ITokenizerFactory ICELANDIC = new MosesTokenizerFactory("is");
    public static final ITokenizerFactory ITALIAN = new MosesTokenizerFactory("it");
    public static final ITokenizerFactory LATVIAN = new MosesTokenizerFactory("lv");
    public static final ITokenizerFactory DUTCH = new MosesTokenizerFactory("nl");
    public static final ITokenizerFactory POLISH = new MosesTokenizerFactory("pl");
    public static final ITokenizerFactory PORTUGUESE = new MosesTokenizerFactory("pt");
    public static final ITokenizerFactory ROMANIAN = new MosesTokenizerFactory("ro");
    public static final ITokenizerFactory RUSSIAN = new MosesTokenizerFactory("ru");
    public static final ITokenizerFactory SLOVAK = new MosesTokenizerFactory("sk");
    public static final ITokenizerFactory SLOVENE = new MosesTokenizerFactory("sl");
    public static final ITokenizerFactory SWEDISH = new MosesTokenizerFactory("sv");
    public static final ITokenizerFactory TAMIL = new MosesTokenizerFactory("ta");

    public static final Map<Locale, ITokenizerFactory> ALL = new HashMap<>();

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

    private Process tokenizer = null;
    private Process detokenizer = null;

    private OutputStream tokenizerStdin;
    private BufferedReader tokenizerStdout = null;
    private OutputStream detokenizerStdin;
    private BufferedReader detokenizerStdout = null;

    public MosesTokenizer(String languageCode) {
        File moses = new File(ITokenizer.MODELS_PATH, "moses");
        File scripts = new File(moses, "scripts");
        File tokenizerScript = new File(scripts, "tokenizer.perl");
        File detokenizerScript = new File(scripts, "detokenizer.perl");

        String tokenizerCommand = "perl " + tokenizerScript.getAbsolutePath() + " -b -X -l " + languageCode + " -no-escape";
        String detokenizerCommand = "perl " + detokenizerScript.getAbsolutePath() + " -b -l " + languageCode;

        Runtime runtime = Runtime.getRuntime();

        try {
            this.tokenizer = runtime.exec(tokenizerCommand);
            this.detokenizer = runtime.exec(detokenizerCommand);

            this.tokenizerStdin = tokenizer.getOutputStream();
            this.tokenizerStdout = new BufferedReader(new InputStreamReader(tokenizer.getInputStream(), "UTF-8"));
            this.detokenizerStdin = detokenizer.getOutputStream();
            this.detokenizerStdout = new BufferedReader(new InputStreamReader(detokenizer.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            this.close();
            throw new RuntimeException("Error while executing processes", e);
        }
    }

    @Override
    public String[] tokenize(String text) {
        String detokenized;

        try {
            this.detokenizerStdin.write(text.getBytes("utf-8"));
            this.detokenizerStdin.write('\n');
            this.detokenizerStdin.flush();

            detokenized = this.detokenizerStdout.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Error while running detokenizer", e);
        }

        String tokenized;

        try {
            this.tokenizerStdin.write(detokenized.getBytes("utf-8"));
            this.tokenizerStdin.write('\n');
            this.tokenizerStdin.flush();

            tokenized = this.tokenizerStdout.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Error while running detokenizer", e);
        }

        return tokenized.split("\\s+");
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(tokenizerStdin);
        IOUtils.closeQuietly(tokenizerStdout);
        IOUtils.closeQuietly(detokenizerStdin);
        IOUtils.closeQuietly(detokenizerStdout);

        if (tokenizer != null)
            tokenizer.destroy();
        if (detokenizer != null)
            detokenizer.destroy();
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
    }

}
