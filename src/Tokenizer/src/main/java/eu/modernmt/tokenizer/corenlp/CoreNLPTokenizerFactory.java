package eu.modernmt.tokenizer.corenlp;

import edu.stanford.nlp.process.TokenizerFactory;
import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;

/**
 * Created by davide on 13/11/15.
 */
class CoreNLPTokenizerFactory extends ITokenizerFactory {

    private TokenizerFactory<?> factory;

    public CoreNLPTokenizerFactory(TokenizerFactory<?> factory) {
        this(factory, null);
    }

    public CoreNLPTokenizerFactory(TokenizerFactory<?> factory, String options) {
        this.factory = factory;

        if (options != null)
            factory.setOptions(options);
    }

    @Override
    public ITokenizer newInstance() {
        return new CoreNLPTokenizer(factory);
    }
}
