package eu.modernmt.tokenizer;

/**
 * Created by davide on 13/11/15.
 */
public abstract class IDetokenizerFactory {

    public IDetokenizer create() {
        return this.newInstance();
    }

    protected abstract IDetokenizer newInstance();

}
