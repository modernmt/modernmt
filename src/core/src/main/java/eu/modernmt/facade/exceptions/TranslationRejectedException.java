package eu.modernmt.facade.exceptions;

public class TranslationRejectedException extends TranslationException {

    public TranslationRejectedException() {
        super("Failed to submit new translation: service temporarily overloaded");
    }
}
