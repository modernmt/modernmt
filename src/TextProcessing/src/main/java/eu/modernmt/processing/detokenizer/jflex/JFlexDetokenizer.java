package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.detokenizer.Detokenizer;
import eu.modernmt.processing.detokenizer.MultiInstanceDetokenizer;
import eu.modernmt.processing.detokenizer.jflex.annotators.ItalianAnnotator;
import eu.modernmt.processing.framework.ProcessingException;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.regex.Pattern;

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

            annotator.reset(text.getReader());

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

    private static String process(JFlexDetokenizer detokenizer, String line) throws ProcessingException {
        String[] tokens = line.split("\\s+");
        Token[] words = new Token[tokens.length];

        for (int i = 0; i < tokens.length; i++)
            words[i] = new Token(tokens[i], true);

        Translation translation = new Translation(words, null, null);
        detokenizer.call(translation);

        return translation.toString();
    }


    private static void process(InputStream input, OutputStream output) throws IOException, ProcessingException {
        JFlexDetokenizer detokenizer = new JFlexDetokenizer();

        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));

        String line;
        while ((line = reader.readLine()) != null) {
            String string = process(detokenizer, line);

            output.write(string.getBytes("UTF-8"));
            output.write('\n');
        }
    }

    public static void main(String[] args) throws Throwable {
        System.setProperty("mmt.home", "/Users/davide/workspaces/mmt/ModernMT/");

        FileInputStream input = null;
        FileOutputStream output = null;

        try {
            input = new FileInputStream("/Users/davide/Desktop/tokenizer/text.tok.en");
            output = new FileOutputStream("/Users/davide/Desktop/tokenizer/text.jflex.en");

            process(input, output);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }

//        String text1 = "in the cantons of Olette and Arles-sur-Tech in the department of Pyrénées-Orientales;";
//        String text = "&apos; Submit （ 送信 ） &apos; をクリックし 、 リクエストを完了してください 。 あるいは 、 &apos; Cancel ( 取消 ） &apos; をクリックして再度リクエストをプロセスしてください 。";
//        String gold = "&apos;Submit （ 送信 ）&apos; をクリックし、リクエストを完了してください。あるいは、 &apos;Cancel (取消 ）&apos; をクリックして再度リクエストをプロセスしてください。";
//        System.out.println(process(new JFlexDetokenizer(), text1));
//        System.out.println(process(new JFlexDetokenizer(), text));
//        System.out.println(gold);
    }

}
