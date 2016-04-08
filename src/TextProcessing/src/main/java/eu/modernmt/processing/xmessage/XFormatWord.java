package eu.modernmt.processing.xmessage;

import eu.modernmt.model.Word;
import eu.modernmt.model.xmessage.XFormat;

/**
 * Created by davide on 08/04/16.
 */
public class XFormatWord extends Word {

    private final XFormat format;

    public XFormatWord(XFormat format) {
        super(format.getPlaceholder(), format.getPlaceholder());
        this.format = format;
    }

    public XFormatWord(XFormat format, String rightSpace) {
        super(format.getPlaceholder(), format.getPlaceholder(), rightSpace);
        this.format = format;
    }

    public XFormatWord(XFormat format, String rightSpace, boolean rightSpaceRequired) {
        super(format.getPlaceholder(), format.getPlaceholder(), rightSpace, rightSpaceRequired);
        this.format = format;
    }

    public XFormat getFormat() {
        return format;
    }
}
