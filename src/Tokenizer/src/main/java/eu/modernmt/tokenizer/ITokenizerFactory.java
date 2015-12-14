package eu.modernmt.tokenizer;

/**
 * Created by davide on 13/11/15.
 */
public abstract class ITokenizerFactory {

    public ITokenizer create() {
        ITokenizer tokenizer = this.newInstance();
        return tokenizer;
    }

    protected abstract ITokenizer newInstance();

}
