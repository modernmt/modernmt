package eu.modernmt.processing.tokenizer.hebmorph;

import eu.modernmt.processing.Languages;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.util.LuceneTokenizerAdapter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 13/11/15.
 */
public class HebMorphTokenizer extends LuceneTokenizerAdapter {

    public static final HebMorphTokenizer HEBREW = new HebMorphTokenizer();

    public static final Map<Locale, Tokenizer> ALL = new HashMap<>();

    static {
        ALL.put(Languages.HEBREW, HEBREW);
    }

    private HebMorphTokenizer() {
        super(HebrewAnalyzer.class);
    }
}
