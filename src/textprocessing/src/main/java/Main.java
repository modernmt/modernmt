import eu.modernmt.io.LineReader;
import eu.modernmt.model.Languages;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.util.TokensOutputter;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by andrea on 02/03/17.
 */
public class Main {

    public static void main(String[] args) throws Throwable {
        buildAll();
    }


    private static void buildAll() throws Throwable {
        File folder = new File("/home/andrea/train/");
        Collection<Corpus> corpora = Corpora.list(Languages.ENGLISH, folder);

        PrintWriter outputToStringWriter = new PrintWriter("/home/andrea/new-output-tostring2.txt", "UTF-8");
        PrintWriter outputTokenOutputterWriter = new PrintWriter("/home/andrea/new-output-tokenoutputter2.txt", "UTF-8");
        Preprocessor preprocessor = new Preprocessor(Languages.ENGLISH);

        for (Corpus input_file : corpora) {

            System.out.println(input_file.getName());
            LineReader reader = input_file.getContentReader();
            String sCurrentLine = reader.readLine();

            while (sCurrentLine != null) {

                Sentence s = preprocessor.process(sCurrentLine);
                outputTokenOutputterWriter.println(TokensOutputter.toString(s, true, true));
                outputToStringWriter.println(s.toString().trim());

                sCurrentLine = reader.readLine();
            }
        }
        outputToStringWriter.close();
        outputTokenOutputterWriter.close();

        preprocessor.close();

    }


    private static void checkSentence() throws Throwable {

        String input = " 1990 ";


        Pattern NUMERIC_PATTERN = Pattern.compile("[0-9]+");
        Matcher matcher = NUMERIC_PATTERN.matcher(" 1990 ");
        System.out.println(matcher.find());
        int start = matcher.start();
        int end = matcher.end();

        String zeroesString = StringUtils.repeat("0", end - start);
        System.out.println(zeroesString);

        Preprocessor preprocessor = new Preprocessor(Languages.ENGLISH);

        Sentence s = preprocessor.process(input);
        System.out.println(s.getStrippedString(true));

        preprocessor.close();
    }


    private static void whatCharIsThis() {
        char c1_old = ' ';
        char c2_old = ' ';
        System.out.println((int) c1_old);   //32
        System.out.println((int) c2_old);   //160

        char c1_new = ' ';
        System.out.println((int) c1_new);   //32

        String prova = c2_old + "ciao a tutti" + c2_old;
        System.out.println(prova.trim());
    }
}