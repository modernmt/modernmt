package eu.modernmt.decoder;

public class TranslationTimeoutException extends DecoderException {

    public TranslationTimeoutException() {
        super("Translation request timed out");
    }

}
