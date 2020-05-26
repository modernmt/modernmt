package eu.modernmt.model.corpus;

/**
 * Created by davide on 28/08/17.
 */
public interface CorpusWrapper extends Corpus {

    Corpus getWrappedCorpus();

    static Corpus unwrap(Corpus corpus) {
        while (corpus instanceof CorpusWrapper) {
            corpus = ((CorpusWrapper) corpus).getWrappedCorpus();
        }

        return corpus;
    }

}
