package eu.modernmt.model.corpus;

/**
 * Created by davide on 28/08/17.
 */
public interface MultilingualCorpusWrapper extends MultilingualCorpus {

    MultilingualCorpus getWrappedCorpus();

}
