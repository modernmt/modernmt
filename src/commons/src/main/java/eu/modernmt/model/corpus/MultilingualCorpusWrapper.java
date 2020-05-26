package eu.modernmt.model.corpus;

/**
 * Created by davide on 28/08/17.
 */
public interface MultilingualCorpusWrapper extends MultilingualCorpus {

    MultilingualCorpus getWrappedCorpus();

    static MultilingualCorpus unwrap(MultilingualCorpus corpus) {
        while (corpus instanceof MultilingualCorpusWrapper) {
            corpus = ((MultilingualCorpusWrapper) corpus).getWrappedCorpus();
        }

        return corpus;
    }

}
