package eu.modernmt.tokenizer.hebmorph;

import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;
import eu.modernmt.tokenizer.Languages;
import eu.modernmt.tokenizer.lucene.LuceneTokenizer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 13/11/15.
 */
public class HebMorphTokenizer extends LuceneTokenizer {

    public static final ITokenizerFactory HEBREW = new ITokenizerFactory() {
        @Override
        protected ITokenizer newInstance() {
            return new HebMorphTokenizer();
        }
    };

    public static final Map<Locale, ITokenizerFactory> ALL = new HashMap<>();

    static {
        ALL.put(Languages.HEBREW, HEBREW);
    }

    public HebMorphTokenizer() {
        super(new HebrewAnalyzer());
    }
}
