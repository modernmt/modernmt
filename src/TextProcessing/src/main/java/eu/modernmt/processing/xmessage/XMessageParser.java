package eu.modernmt.processing.xmessage;

import eu.modernmt.processing.framework.LanguageNotSupportedException;
import eu.modernmt.processing.framework.TextProcessor;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 08/04/16.
 */
public class XMessageParser extends TextProcessor<String, String> {

    public static final String FORMATS_KEY = "XMessageParser.formats";

    public XMessageParser(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public String call(String string, Map<String, Object> metadata) {
        try {
            XMessage xmessage = XMessage.parse(string);
            metadata.put(FORMATS_KEY, xmessage.getFormats());
            return xmessage.toString();
        } catch (XMessageFormatException e) {
            // If not a valid XMessage it just keeps string as it is.
            return string;
        }
    }

}
