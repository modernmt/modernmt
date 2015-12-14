package eu.modernmt.tokenizer.kuromoji;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;
import eu.modernmt.tokenizer.Languages;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 11/11/15.
 */
public class KuromojiTokenizer extends ITokenizer {

    public static final ITokenizerFactory JAPANESE = new ITokenizerFactory() {
        @Override
        protected ITokenizer newInstance() {
            return new KuromojiTokenizer();
        }
    };

    public static final Map<Locale, ITokenizerFactory> ALL = new HashMap<>();

    static {
        ALL.put(Languages.JAPANESE, JAPANESE);
    }

    private Tokenizer tokenizer = new Tokenizer();

    @Override
    public String[] tokenize(String text) {
        List<Token> tokens = tokenizer.tokenize(text);
        String[] array = new String[tokens.size()];

        for (int i = 0; i < array.length; i++)
            array[i] = tokens.get(i).getSurface();

        return array;
    }

}
