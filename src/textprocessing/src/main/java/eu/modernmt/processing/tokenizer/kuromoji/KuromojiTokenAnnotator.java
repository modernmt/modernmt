package eu.modernmt.processing.tokenizer.kuromoji;

import com.atilika.kuromoji.ipadic.Token;
import eu.modernmt.processing.tokenizer.TokenizedString;
import eu.modernmt.processing.tokenizer.BaseTokenizer;

import java.util.List;

public class KuromojiTokenAnnotator implements BaseTokenizer.Annotator {

    private com.atilika.kuromoji.ipadic.Tokenizer tokenizer = new com.atilika.kuromoji.ipadic.Tokenizer();

    @Override
    public void annotate(TokenizedString string) {
        List<Token> tokens = tokenizer.tokenize(string.toString());
        tokens.removeIf(token -> token.getSurface().trim().length() == 0);

        for (Token token : tokens)
            string.setWord(token.getPosition(), token.getPosition() + token.getSurface().length());
    }
}
