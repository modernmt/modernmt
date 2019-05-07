package eu.modernmt.decoder.neural.memory.lucene.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

import java.util.regex.Pattern;

public class PunctuationFilter extends FilteringTokenFilter {

    private static final Pattern REGEX = Pattern.compile("\\p{Punct}");

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public PunctuationFilter(TokenStream in) {
        super(in);
    }

    @Override
    protected boolean accept() {
        return !REGEX.matcher(termAtt).matches();
    }

}
