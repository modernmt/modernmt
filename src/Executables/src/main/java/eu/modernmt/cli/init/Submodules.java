package eu.modernmt.cli.init;

import eu.modernmt.aligner.AlignerFactory;
import eu.modernmt.aligner.fastalign.FastAlignFactory;
import eu.modernmt.context.ContextAnalyzerFactory;
import eu.modernmt.context.lucene.LuceneAnalyzerFactory;
import eu.modernmt.decoder.DecoderFactory;
import eu.modernmt.decoder.moses.MosesDecoderFactory;

/**
 * Created by davide on 09/05/16.
 */
public class Submodules {

    public static void link() {
        DecoderFactory.registerFactory(MosesDecoderFactory.class);
        ContextAnalyzerFactory.registerFactory(LuceneAnalyzerFactory.class);
        AlignerFactory.registerFactory(FastAlignFactory.class);
    }

}
