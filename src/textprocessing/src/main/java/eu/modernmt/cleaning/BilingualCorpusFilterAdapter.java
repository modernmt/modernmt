package eu.modernmt.cleaning;

/**
 * Created by davide on 06/12/16.
 */
public abstract class BilingualCorpusFilterAdapter implements BilingualCorpusFilter {

    @Override
    public FilterInitializer getInitializer() {
        return null;
    }

    @Override
    public void onInitStart() {
    }

    @Override
    public void onInitEnd() {
    }

}
