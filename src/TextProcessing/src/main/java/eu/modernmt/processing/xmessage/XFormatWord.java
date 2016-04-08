package eu.modernmt.processing.xmessage;

import eu.modernmt.model.Word;
import eu.modernmt.model.xmessage.XFormat;

/**
 * Created by davide on 08/04/16.
 */
public class XFormatWord extends Word {

    private static final Transformation TRANSFORMATION = (Transformation) (source, target) -> {
        if (source != null && source instanceof XFormatWord && target instanceof XFormatWord) {
            XFormat format = ((XFormatWord) source).getFormat();
            ((XFormatWord) target).setFormat(format);
        }
    };

    private XFormat format;

    public XFormatWord(XFormat format) {
        super(format.getPlaceholder(), format.getPlaceholder());
        this.format = format;
        this.setTransformation(TRANSFORMATION);
    }

    public XFormatWord(XFormat format, String rightSpace) {
        super(format.getPlaceholder(), format.getPlaceholder(), rightSpace);
        this.format = format;
        this.setTransformation(TRANSFORMATION);
    }

    public XFormatWord(XFormat format, String rightSpace, boolean rightSpaceRequired) {
        super(format.getPlaceholder(), format.getPlaceholder(), rightSpace, rightSpaceRequired);
        this.format = format;
        this.setTransformation(TRANSFORMATION);
    }

    public XFormatWord(String text, String placeholder, String rightSpace, boolean rightSpaceRequired) {
        super(text, placeholder, rightSpace, rightSpaceRequired);
        this.setTransformation(TRANSFORMATION);
    }

    public XFormat getFormat() {
        return format;
    }

    public void setFormat(XFormat format) {
        this.format = format;
    }

    @Override
    public String getText() {
        return format == null ? super.getText() : format.toString();
    }

    @Override
    public boolean hasText() {
        return format != null || super.hasText();
    }

    @Override
    public String getPlaceholder() {
        return format == null ? super.getPlaceholder() : format.getPlaceholder();
    }

    @Override
    public String toString() {
        return format == null ? super.toString() : format.toString();
    }

}
