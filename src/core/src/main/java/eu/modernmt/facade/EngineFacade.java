package eu.modernmt.facade;

import java.util.Locale;

/**
 * Created by davide on 24/10/16.
 */
public class EngineFacade {

    public Locale getSourceLanguage() {
        return ModernMT.node.getEngine().getSourceLanguage();
    }

    public Locale getTargetLanguage() {
        return ModernMT.node.getEngine().getTargetLanguage();
    }

}
