package eu.modernmt.tokenizer.moses;

import eu.modernmt.tokenizer.IDetokenizer;
import eu.modernmt.tokenizer.IDetokenizerFactory;

/**
 * Created by davide on 23/11/15.
 */
public class MosesDetokenizerFactory extends IDetokenizerFactory {

    private String lang;

    public MosesDetokenizerFactory(String lang) {
        this.lang = lang;
    }

    @Override
    public IDetokenizer newInstance() {
        return new MosesDetokenizer(lang);
    }
}
