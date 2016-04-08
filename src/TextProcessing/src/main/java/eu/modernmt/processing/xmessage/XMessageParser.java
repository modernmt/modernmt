package eu.modernmt.processing.xmessage;

import eu.modernmt.model.xmessage.XMessage;
import eu.modernmt.model.xmessage.XMessageFormatException;
import eu.modernmt.processing.framework.TextProcessor;

/**
 * Created by davide on 08/04/16.
 */
public class XMessageParser implements TextProcessor<String, String> {

    @Override
    public String call(String string) {
        try {
            XMessage xmessage = XMessage.parse(string);
            return xmessage.toString();
        } catch (XMessageFormatException e) {
            // If not a valid XMessage it just keeps string as it is.
            return string;
        }
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
