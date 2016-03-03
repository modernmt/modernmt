package eu.modernmt.config;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Created by davide on 24/02/16.
 */
public class CharsetConfig {

    private final Charset charset;

    CharsetConfig() {
        try {
            charset = Charset.forName("UTF-8");
        } catch (UnsupportedCharsetException e) {
            throw new Error("Current system does not support UTF-8");
        }
    }

    public Charset get() {
        return charset;
    }
}
