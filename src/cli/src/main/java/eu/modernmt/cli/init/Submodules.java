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

    // TODO: completely remove this implementation

    public static final String DECODER_FACTORY_PROPERTY = "mmt.decoder.factory";
    public static final String CONTEXT_FACTORY_PROPERTY = "mmt.context.factory";
    public static final String ALIGNER_FACTORY_PROPERTY = "mmt.aligner.factory";

    public static void link() {
        DecoderFactory.registerFactory(getDecoderFactory());
        ContextAnalyzerFactory.registerFactory(getAnalyzerFactory());
        AlignerFactory.registerFactory(getAlignerFactory());
    }

    private static Class<? extends DecoderFactory> getDecoderFactory() {
        Class<? extends DecoderFactory> cls = loadClass(DECODER_FACTORY_PROPERTY);
        return cls == null ? MosesDecoderFactory.class : cls;
    }

    private static Class<? extends ContextAnalyzerFactory> getAnalyzerFactory() {
        Class<? extends ContextAnalyzerFactory> cls = loadClass(CONTEXT_FACTORY_PROPERTY);
        return cls == null ? LuceneAnalyzerFactory.class : cls;
    }

    private static Class<? extends AlignerFactory> getAlignerFactory() {
        Class<? extends AlignerFactory> cls = loadClass(ALIGNER_FACTORY_PROPERTY);
        return cls == null ? FastAlignFactory.class : cls;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> loadClass(String property) {
        String className = System.getProperty(property);
        if (className == null)
            className = System.getenv(property);

        if (className != null && className.trim().isEmpty())
            className = null;

        if (className == null) {
            return null;
        } else {
            try {
                return (Class<? extends T>) Class.forName(className);
            } catch (ClassCastException | ClassNotFoundException e) {
                throw new IllegalArgumentException("Invalid calue for property " + property);
            }
        }
    }

}
