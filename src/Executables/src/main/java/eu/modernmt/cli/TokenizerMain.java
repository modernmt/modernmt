package eu.modernmt.cli;

import eu.modernmt.tokenizer.Languages;
import eu.modernmt.tokenizer.TokenizerPool;
import eu.modernmt.tokenizer.moses.MosesTokenizer;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class TokenizerMain {

    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) throws IOException {
        tokenize(args[0], System.in, System.out);
    }

    private static void tokenize(String lang, InputStream input, OutputStream output) throws IOException {
        Locale language = Languages.getSupportedLanguage(lang);

        if (language == null)
            throw new IllegalArgumentException("Unsupported language: " + lang);

        TokenizerPool tokenizer = TokenizerPool.getCachedInstance(language);

        ArrayList<String> batch = new ArrayList<>(BATCH_SIZE);

        try {
            LineIterator stdin = IOUtils.lineIterator(input, "UTF-8");
            while (stdin.hasNext()) {
                batch.add(stdin.next());

                if (batch.size() >= BATCH_SIZE) {
                    process(tokenizer, batch, output);
                    batch.clear();
                }
            }

            if (batch.size() > 0)
                process(tokenizer, batch, output);
        } finally {
            tokenizer.terminate();
        }
    }

    private static void process(TokenizerPool tokenizer, ArrayList<String> batch, OutputStream output) throws IOException {
        List<String[]> tokens = tokenizer.tokenize(batch);

        for (String[] line : tokens) {
            for (String token : line) {
                output.write(token.getBytes("UTF-8"));
                output.write(' ');
            }

            output.write('\n');
        }
    }
}
