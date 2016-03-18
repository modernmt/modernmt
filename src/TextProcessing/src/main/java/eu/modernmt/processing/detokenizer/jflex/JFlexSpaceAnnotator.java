package eu.modernmt.processing.detokenizer.jflex;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 29/01/16.
 */
public abstract class JFlexSpaceAnnotator {

    public static final int YYEOF = -1;
    public static final int REMOVE_FIRST = 0;
    public static final int REMOVE_LAST = 1;
    public static final int REMOVE_ALL = 2;

    protected int zzStartReadOffset = 0;

    public final void annotate(SpacesAnnotatedString text, int tokenType) {
        int zzMarkedPos = getMarkedPosition();

        int zzStartRead = getStartRead() + zzStartReadOffset;
        zzStartReadOffset = 0;

        int yychar = yychar();
        int offset = 0;

        if (yychar > zzStartRead) {
            offset = yychar + zzStartRead;
        }

        switch (tokenType) {
            case REMOVE_FIRST:
                text.removeSpaceRight(offset + zzStartRead);
                break;
            case REMOVE_LAST:
                text.removeSpaceLeft(offset + zzMarkedPos - 1);
                break;
            case REMOVE_ALL:
                text.removeAllSpaces(offset + zzStartRead, offset + zzMarkedPos);
                break;
        }

        yypushback(1);
    }

    public final void reset(Reader reader) {
        onReset();
        this.yyreset(reader);
    }

    protected void onReset() {

    }

    public abstract int next() throws IOException;

    protected abstract void yyreset(Reader reader);

    protected abstract void yypushback(int number);

    protected abstract int getStartRead();

    protected abstract int getMarkedPosition();

    protected abstract int yychar();

}
