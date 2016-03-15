package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.detokenizer.Detokenizer;
import eu.modernmt.processing.detokenizer.MultiInstanceDetokenizer;
import eu.modernmt.processing.detokenizer.jflex.annotators.ItalianAnnotator;
import eu.modernmt.processing.framework.ProcessingException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

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

    public static void main(String[] args) throws Throwable {
        System.setProperty("mmt.home", "/Users/davide/workspaces/mmt/ModernMT/");
        JFlexDetokenizer detokenizer = new JFlexDetokenizer();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line=br.readLine()) != null) {
	    String[] splitStr = line.split("\\s+");
	    List<Token> tokenList = new ArrayList<Token>();
	    for (String str : splitStr) {
		tokenList.add(new Token(str, true));
	    }
	    Token[] tokens = new Token[ tokenList.size() ];
	    tokenList.toArray(tokens);
	    Translation translation = new Translation(tokens, null, null);
	    detokenizer.call(translation);
	    System.out.println(translation);
	}

	/*
        Translation translation = new Translation(new Token[] {
                new Token("Un", true),
                new Token("bell'", true),
                new Token("esempio", true),
                new Token("!", true),
        }, null, null);

        System.out.println(translation);
        detokenizer.call(translation);
        System.out.println(translation);
	*/
    }

}
