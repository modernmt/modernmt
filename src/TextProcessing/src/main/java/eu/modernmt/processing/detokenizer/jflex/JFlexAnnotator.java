package eu.modernmt.processing.detokenizer.jflex;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 29/01/16.
 */
public abstract class JFlexAnnotator {

    public static final int YYEOF = -1;
    public static final int REMOVE = 0;

    protected int zzStartReadOffset = 0;

    public final void annotate(AnnotatedString text, int tokenType) {
        int zzMarkedPos = getMarkedPosition();

        int zzStartRead = getStartRead() + zzStartReadOffset;
        zzStartReadOffset = 0;

        int yychar = yychar();
        int offset = 0;

        if (yychar > zzStartRead) {
            offset = yychar + zzStartRead;
        }

        switch (tokenType) {
            case REMOVE:
                text.removeSpace(offset + zzStartRead);
                break;
        }
    }

    public abstract void yyreset(Reader reader);

    public abstract int next() throws IOException;

    protected abstract int getStartRead();

    protected abstract int getMarkedPosition();

    protected abstract int yychar();

}
