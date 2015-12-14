package eu.modernmt.tokenizer.lucene;

import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;
import org.apache.lucene.analysis.Analyzer;

/**
 * Created by davide on 13/11/15.
 */
class LuceneTokenizerFactory extends ITokenizerFactory {

    private Class<? extends Analyzer> analyzerClass;

    public LuceneTokenizerFactory(Class<? extends Analyzer> analyzerClass) {
        this.analyzerClass = analyzerClass;
    }

    @Override
    public ITokenizer newInstance() {
        try {
            Analyzer analyzer = this.analyzerClass.newInstance();
            return new LuceneTokenizer(analyzer);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Error during class instantiation: " + this.analyzerClass.getName(), e);
        }
    }

}
