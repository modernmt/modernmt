package eu.modernmt.processing.detokenizer;

import eu.modernmt.processing.detokenizer.jflex.JFlexDetokenizer;

import java.util.Locale;

/**
 * Created by davide on 27/01/16.
 */
public class Detokenizers {

    public static Detokenizer forLanguage(Locale language) {
        Detokenizer detokenizer = JFlexDetokenizer.ALL.get(language);
        return detokenizer == null ? JFlexDetokenizer.DEFAULT : detokenizer;
    }

}
