package eu.modernmt.io;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Created by davide on 31/08/16.
 */
public class DefaultCharset {

    private static Charset charset = null;

    public static Charset get() {
        if (charset == null) {
            try {
                charset = Charset.forName("UTF-8");
            } catch (UnsupportedCharsetException e) {
                throw new Error("Current system does not support UTF-8");
            }
        }

        return charset;
    }

}
