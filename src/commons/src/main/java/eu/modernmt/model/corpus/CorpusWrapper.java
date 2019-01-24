package eu.modernmt.model.corpus;

/**
 * Created by davide on 28/08/17.
 */
public interface CorpusWrapper extends Corpus {

    Corpus getWrappedCorpus();

}
