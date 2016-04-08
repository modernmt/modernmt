package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.framework.ProcessingPipeline;
import eu.modernmt.processing.xmessage.XFormatWord;
import org.apache.commons.io.IOUtils;

import java.util.Locale;

/**
 * Created by davide on 08/04/16.
 */
public class __XMessageTest {

    public static void main(String[] args) throws Throwable {
        String string = "Clear {0} {0,choice,singular#example|plural#examples}";
        ProcessingPipeline<String, Sentence> preprocessing = null;
        ProcessingPipeline<Translation, Void> postprocessing = null;

        try {
            preprocessing = Preprocessor.getPipeline(Locale.ENGLISH, true);
            Sentence sentence = preprocessing.process(string);

            System.out.println("SOURCE:");
            for (Word word : sentence.getWords()) {
                System.out.print(word.getPlaceholder());
                if (word instanceof XFormatWord)
                    System.out.print(" -> " + ((XFormatWord) word).getFormat().toString());
                System.out.println();
            }
            System.out.println();


            System.out.println("TRANSLATION:");
            postprocessing = Postprocessor.getPipeline(Locale.ITALIAN, true);
            Translation translation = new Translation(new Word[]{
                    new Word("Cancella", " "),
                    new Word("{0}", " "),
                    new Word("{1,choice}"),
            }, sentence, new int[][]{
                    {0, 0},
                    {1, 1},
                    {2, 2},
            });
            postprocessing.process(translation);

            for (Word word : translation.getWords()) {
                System.out.print(word.getPlaceholder());
                if (word instanceof XFormatWord)
                    System.out.print(" -> " + ((XFormatWord) word).getFormat().toString());
                System.out.println();
            }
            System.out.println();

            System.out.println(translation);
        } finally {
            IOUtils.closeQuietly(preprocessing);
            IOUtils.closeQuietly(postprocessing);
        }
    }

}
