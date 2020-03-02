package eu.modernmt.processing;

import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Sentence;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class PreprocessSpecialEntitiesTest {

    private static final LanguageDirection language = new LanguageDirection(Language.ENGLISH, Language.ITALIAN);
    private static Preprocessor preprocessor = null;

    @BeforeClass
    public static void setupPreprocessor() throws IOException {
        preprocessor = new Preprocessor();
    }

    @AfterClass
    public static void teardownPreprocessor() {
        if (preprocessor != null)
            preprocessor.close();
    }

    private static void test(String text, String expected) throws ProcessingException {
        Sentence sentence = preprocessor.process(language, text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals(expected, TokensOutputStream.serialize(sentence, false, true));
    }

    @Test
    public void testDoNotTranslate() throws ProcessingException {
        test("List: ${DNT1}, ${DNT2} and ${DNT3}",
                "List : ${DNT1} , ${DNT2} and ${DNT3}");
    }

    @Test
    public void testPipesDelimiter() throws ProcessingException {
        test("That's | a || very ||| long |||| example . |||||",
                "That 's | a || very ||| long |||| example . |||||");
    }

    @Test
    public void testVarsLikeTokens() throws ProcessingException {
        test("${link_start}Visit %{user_name}'s profile${link_end}.%{terms_of_service_link}",
                "${link_start} Visit %{user_name} 's profile ${link_end} . %{terms_of_service_link}");
    }

    @Test
    public void testAndroidVarsTokens() throws ProcessingException {
        test("That's %f %s", "That 's %f %s");
        test("Hello %1$s, tab %2$d out of %3$f. %1$s:%2$s",
                "Hello %1$s , tab %2$d out of %3$f . %1$s : %2$s");
        test("%1$s (%2$.1f average)",
                "%1$s ( %2$.1f average )");
    }

    @Test
    public void testCombined() throws ProcessingException {
        test("option %{option_one} |||| option %{option_two}",
                "option %{option_one} |||| option %{option_two}");
        test("option %1$s |||| option %2$f",
                "option %1$s |||| option %2$f");
    }

}
