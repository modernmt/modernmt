package eu.modernmt.processing.tokenizer.kuromoji;

import com.atilika.kuromoji.ipadic.Token;
import eu.modernmt.processing.tokenizer.TokenizedString;
import eu.modernmt.processing.tokenizer.BaseTokenizer;

import java.util.List;

public class KuromojiTokenAnnotator implements BaseTokenizer.Annotator {

    private static com.atilika.kuromoji.ipadic.Tokenizer tokenizer = null;

    private static com.atilika.kuromoji.ipadic.Tokenizer getTokenizer() {
        if (tokenizer == null) {
            synchronized (KuromojiTokenAnnotator.class) {
                if (tokenizer == null)
                    tokenizer = new com.atilika.kuromoji.ipadic.Tokenizer();
            }
        }

        return tokenizer;
    }

    @Override
    public void annotate(TokenizedString string) {
        List<Token> tokens = getTokenizer().tokenize(string.toString());
        tokens.removeIf(token -> token.getSurface().trim().length() == 0);

        for (Token token : tokens)
            string.setWord(token.getPosition(), token.getPosition() + token.getSurface().length());
    }
}
