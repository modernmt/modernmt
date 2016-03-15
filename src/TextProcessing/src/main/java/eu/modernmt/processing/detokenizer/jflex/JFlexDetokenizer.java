package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.detokenizer.Detokenizer;
import eu.modernmt.processing.detokenizer.MultiInstanceDetokenizer;
import eu.modernmt.processing.detokenizer.jflex.annotators.ItalianAnnotator;
import eu.modernmt.processing.framework.ProcessingException;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 29/01/16.
 */
public class JFlexDetokenizer extends MultiInstanceDetokenizer {

    private static class JFlexDetokenizerFactory implements DetokenizerFactory {

        @Override
        public Detokenizer newInstance() {
            return new JFlexDetokenizerImpl();
        }
    }

    public JFlexDetokenizer() {
        super(new JFlexDetokenizerFactory());
    }

    private static class JFlexDetokenizerImpl implements Detokenizer {

        private JFlexAnnotator annotator = new ItalianAnnotator((Reader) null);

        @Override
        public Translation call(Translation translation) throws ProcessingException {
            AnnotatedString text = AnnotatedString.fromTranslation(translation);

            annotator.yyreset(text.getReader());

            int type;
            while ((type = next(annotator)) != JFlexAnnotator.YYEOF) {
                annotator.annotate(text, type);
            }

            text.apply(translation);
            return translation;
        }

        private static int next(JFlexAnnotator annotator) throws ProcessingException {
            try {
                return annotator.next();
            } catch (IOException e) {
                throw new ProcessingException(e);
            }
        }

        @Override
        public void close() {
            // Nothing to do
        }
    }

}
