package eu.modernmt.tokenizer.languagetool;

import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;
import org.languagetool.tokenizers.Tokenizer;

/**
 * Created by davide on 13/11/15.
 */
public class LanguageToolTokenizerFactory extends ITokenizerFactory {

    private Class<? extends Tokenizer> tokenizerClass;

    public LanguageToolTokenizerFactory(Class<? extends Tokenizer> tokenizerClass) {
        this.tokenizerClass = tokenizerClass;
    }

    @Override
    public ITokenizer newInstance() {
        try {
            Tokenizer tokenizer = tokenizerClass.newInstance();
            return new LanguageToolTokenizer(tokenizer);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Error during class instantiation: " + this.tokenizerClass.getName(), e);
        }
    }
}
