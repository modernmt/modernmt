package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.tokenizer.TokenizedString;
import eu.modernmt.processing.tokenizer.BaseTokenizer;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 29/01/16.
 */
public abstract class JFlexTokenAnnotator implements BaseTokenizer.Annotator {

    protected static final int YYEOF = -1;
    protected static final int PROTECT = 0;
    protected static final int PROTECT_ALL = 1;
    protected static final int PROTECT_RIGHT = 2;
    protected static final int WORD = 3;

    protected int zzStartReadOffset = 0;

    @Override
    public final void annotate(TokenizedString text) throws ProcessingException {
        this.yyreset(text.getReader());

        try {
            int type;
            while ((type = next()) != JFlexTokenAnnotator.YYEOF) {
                this.annotate(text, type);
            }
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }

    protected final void annotate(TokenizedString text, int tokenType) {
        int yychar = yychar();

        int begin = yychar + zzStartReadOffset;
        int end = yychar + getMarkedPosition() - getStartRead();
        zzStartReadOffset = 0;

        switch (tokenType) {
            case PROTECT:
                text.protect(begin + 1, end);
                break;
            case PROTECT_ALL:
                text.protect(begin, end);
                break;
            case PROTECT_RIGHT:
                text.protect(end);
                break;
            case WORD:
                text.setWord(begin, end);
                break;
        }
    }

    public abstract void yyreset(Reader reader);

    public abstract int next() throws IOException;

    protected abstract int getStartRead();

    protected abstract int getMarkedPosition();

    protected abstract int yychar();

}
