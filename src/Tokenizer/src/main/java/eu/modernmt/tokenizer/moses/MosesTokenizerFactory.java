package eu.modernmt.tokenizer.moses;

import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;

/**
 * Created by davide on 23/11/15.
 */
public class MosesTokenizerFactory extends ITokenizerFactory {

    private String lang;

    public MosesTokenizerFactory(String lang) {
        this.lang = lang;
    }

    @Override
    public ITokenizer newInstance() {
        return new MosesTokenizer(lang);
    }
}
