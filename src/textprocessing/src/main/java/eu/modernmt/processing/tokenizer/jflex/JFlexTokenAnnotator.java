package eu.modernmt.processing.tokenizer.jflex;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 29/01/16.
 */
public abstract class JFlexTokenAnnotator {

    public static final int YYEOF = -1;
    public static final int PROTECT = 0;
    public static final int PROTECT_ALL = 1;
    public static final int PROTECT_RIGHT = 2;

    protected int zzStartReadOffset = 0;

    public final void annotate(TokensAnnotatedString text, int tokenType) {
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
        }
    }

    public abstract void yyreset(Reader reader);

    public abstract int next() throws IOException;

    protected abstract int getStartRead();

    protected abstract int getMarkedPosition();

    protected abstract int yychar();

}
