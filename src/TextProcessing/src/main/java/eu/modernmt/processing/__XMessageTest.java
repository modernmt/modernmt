package eu.modernmt.processing;

import eu.modernmt.model.*;
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

            System.out.println(translation.getWords()[2].hasRightSpace());
            postprocessing.process(translation);
            System.out.println(translation.getWords()[2].hasRightSpace());


            for (Token token : translation) {
                if (token instanceof MultiOptionsToken) {
                    MultiOptionsToken mop = (MultiOptionsToken) token;

                    if (!mop.hasTranslatedOptions()) {
                        String[] options = mop.getSourceOptions();
                        Translation[] translations = new Translation[options.length];

                        for (int i = 0; i < translations.length; i++) {
                            translations[i] = new Translation(new Word[]{
                                    new Word("__t__" + options[i] + "___", null)
                            }, null, null);
                        }

                        mop.setTranslatedOptions(translations);
                    }
                }
            }


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
