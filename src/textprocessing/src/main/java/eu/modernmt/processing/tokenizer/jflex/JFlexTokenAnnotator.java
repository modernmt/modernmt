package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.TokenizedString;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 29/01/16.
 */
public abstract class JFlexTokenAnnotator implements BaseTokenizer.Annotator {

    protected static final int YYEOF = -1;

    protected static int word(int leftOffset, int rightOffset) {
        return protect(leftOffset, rightOffset, true);
    }

    protected static int protect(int leftOffset, int rightOffset) {
        return protect(leftOffset, rightOffset, false);
    }

    protected static int protect(int leftOffset, int rightOffset, boolean truncate) {
        return ((truncate ? 0x01 : 0x00) << 16) +
                ((leftOffset & 0xFF) << 8) +
                (rightOffset & 0xFF);
    }

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
        boolean truncate = ((tokenType >> 16) & 0xFF) > 0;
        int leftOffset = (tokenType >> 8) & 0xFF;
        int rightOffset = tokenType & 0xFF;

        int begin = yychar();
        int end = begin + getMarkedPosition() - getStartRead();

        int lastIndex;
        if (truncate)
            lastIndex = text.setWord(begin + leftOffset, end - rightOffset);
        else
            lastIndex = text.protect(begin + leftOffset, end - rightOffset);

        if (lastIndex != -1)
            this.yypushback(end - lastIndex - 1);  // set cursor right after last protected char
    }

    public abstract void yyreset(Reader reader);

    public abstract int next() throws IOException;

    protected abstract int getStartRead();

    protected abstract int getMarkedPosition();

    protected abstract int yychar();

    protected abstract void yypushback(int number);

}
