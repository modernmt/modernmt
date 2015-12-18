package eu.modernmt.cli;

import eu.modernmt.tokenizer.DetokenizerPool;
import eu.modernmt.tokenizer.Languages;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class DetokenizerMain {

    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) throws IOException {
        Locale language = Languages.getSupportedLanguage(args[0]);

        if (language == null)
            throw new IllegalArgumentException("Unsupported language: " + args[0]);

        DetokenizerPool detokenizer = DetokenizerPool.getCachedInstance(language);

        ArrayList<String[]> batch = new ArrayList<>(BATCH_SIZE);

        try {
            LineIterator stdin = IOUtils.lineIterator(System.in, "UTF-8");
            while (stdin.hasNext()) {
                batch.add(stdin.next().split("\\s+"));

                if (batch.size() >= BATCH_SIZE) {
                    process(detokenizer, batch);
                    batch.clear();
                }
            }

            if (batch.size() > 0)
                process(detokenizer, batch);
        } finally {
            detokenizer.terminate();
        }
    }

    private static void process(DetokenizerPool detokenizer, ArrayList<String[]> batch) {
        for (String line : detokenizer.detokenize(batch)) {
            System.out.println(line);
        }
    }
}
