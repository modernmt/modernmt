package eu.modernmt.processing.tokenizer.kuromoji;

import com.atilika.kuromoji.ipadic.Token;
import eu.modernmt.processing.AnnotatedString;
import eu.modernmt.processing.Languages;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 11/11/15.
 */
public class KuromojiTokenizer implements Tokenizer {

    public static final KuromojiTokenizer JAPANESE = new KuromojiTokenizer();

    public static final Map<Locale, Tokenizer> ALL = new HashMap<>();

    static {
        ALL.put(Languages.JAPANESE, JAPANESE);
    }

    private com.atilika.kuromoji.ipadic.Tokenizer tokenizer = new com.atilika.kuromoji.ipadic.Tokenizer();

    @Override
    public AnnotatedString call(String text) {
        List<Token> tokens = tokenizer.tokenize(text);
        String[] array = new String[tokens.size()];

        for (int i = 0; i < array.length; i++)
            array[i] = tokens.get(i).getSurface();

        return new AnnotatedString(text, TokenizerOutputTransformer.transform(text, array));
    }

    @Override
    public void close() {
    }

}
