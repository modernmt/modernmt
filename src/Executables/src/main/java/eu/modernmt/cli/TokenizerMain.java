package eu.modernmt.cli;

import eu.modernmt.tokenizer.Languages;
import eu.modernmt.tokenizer.TokenizerPool;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class TokenizerMain {

    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) throws IOException {
        Locale language = Languages.getSupportedLanguage(args[0]);

        if (language == null)
            throw new IllegalArgumentException("Unsupported language: " + args[0]);

        TokenizerPool tokenizer = TokenizerPool.getCachedInstance(language);

        ArrayList<String> batch = new ArrayList<>(BATCH_SIZE);

        try {
            LineIterator stdin = IOUtils.lineIterator(System.in, "UTF-8");
            while (stdin.hasNext()) {
                batch.add(stdin.next());

                if (batch.size() >= BATCH_SIZE) {
                    process(tokenizer, batch);
                    batch.clear();
                }
            }

            if (batch.size() > 0)
                process(tokenizer, batch);
        } finally {
            tokenizer.terminate();
        }
    }

    private static void process(TokenizerPool tokenizer, ArrayList<String> batch) {
        List<String[]> tokens = tokenizer.tokenize(batch);

        for (String[] line : tokens) {
            for (String token : line) {
                System.out.print(token);
                System.out.print(" ");
            }

            System.out.println();
        }
    }
}
