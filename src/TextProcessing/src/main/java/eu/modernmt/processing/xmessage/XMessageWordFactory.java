package eu.modernmt.processing.xmessage;

import eu.modernmt.model.Word;
import eu.modernmt.model.xmessage.XFormat;
import eu.modernmt.processing.SentenceBuilder;

import java.util.Map;

/**
 * Created by davide on 08/04/16.
 */
public class XMessageWordFactory implements SentenceBuilder.WordFactory {

    private XFormat[] formats;
    private int index;

    @Override
    public boolean match(String text, String placeholder) {
        if (formats == null)
            return false;
        if (index >= formats.length)
            return false;

        if (!XFormat.PLACEHOLDER_PATTERN.matcher(placeholder).matches())
            return false;

        return XFormat.extractId(placeholder) == formats[index].id;
    }

    @Override
    public Word build(String text, String placeholder, String rightSpace) {
        return new XFormatWord(formats[index++], rightSpace);
    }

    @Override
    public void setMetadata(Map<String, Object> metadata) {
        this.formats = (XFormat[]) metadata.get(XMessageParser.FORMATS_KEY);
        this.index = 0;
    }

}
