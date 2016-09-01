package eu.modernmt.processing.tokenizer.lucene.analyzers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.hebrew.HebrewTokenizer;
import org.apache.lucene.analysis.hebrew.NiqqudFilter;
import org.apache.lucene.analysis.util.CharArraySet;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 13/11/15.
 */
public class HebrewAnalyzer extends Analyzer {

    private final CharArraySet commonWords;
    private Map<String, char[]> suffixByTokenType;
    private HashMap<String, Integer> prefixesTree;

    public HebrewAnalyzer() {
        this(new HashMap<>(), null);
    }

    public HebrewAnalyzer(HashMap<String, Integer> prefixes) {
        this(prefixes, null);
    }

    public HebrewAnalyzer(HashMap<String, Integer> prefixes, CharArraySet commonWords) {
        this.suffixByTokenType = null;
        this.commonWords = commonWords;
        this.prefixesTree = prefixes;
    }

    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        final HebrewTokenizer src = new HebrewTokenizer(reader, prefixesTree);
        NiqqudFilter tok = new NiqqudFilter(src);
        return new TokenStreamComponents(src, tok) {
            protected void setReader(Reader reader) throws IOException {
                super.setReader(reader);
            }
        };
    }

    public void registerSuffix(String tokenType, String suffix) {
        if (this.suffixByTokenType == null) {
            this.suffixByTokenType = new HashMap<>();
        }

        if (!this.suffixByTokenType.containsKey(tokenType)) {
            this.suffixByTokenType.put(tokenType, suffix.toCharArray());
        }
    }

}
