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
        int yychar = yychar();

        int begin = yychar + zzStartReadOffset;
        int end = yychar + getMarkedPosition() - getStartRead();
        zzStartReadOffset = 0;

        switch (tokenType) {
            case REMOVE_FIRST:
                text.removeSpaceRight(begin);
                break;
            case REMOVE_LAST:
                text.removeSpaceLeft(end - 1);
                break;
            case REMOVE_ALL:
                text.removeAllSpaces(begin, end);
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
