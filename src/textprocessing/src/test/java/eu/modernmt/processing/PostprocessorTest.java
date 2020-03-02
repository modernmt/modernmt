package eu.modernmt.processing;

import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class PostprocessorTest {
    private static final LanguageDirection language = new LanguageDirection(Language.ENGLISH, Language.ITALIAN);
    private static Preprocessor preprocessor = null;
    private static Postprocessor postprocessor = null;

    @BeforeClass
    public static void setupPreprocessor() throws IOException {
        preprocessor = new Preprocessor();
    }

    @BeforeClass
    public static void setupPostprocessor() throws IOException {
        postprocessor = new Postprocessor();
    }

    @AfterClass
    public static void teardownPreprocessor() {
        if (preprocessor != null)
            preprocessor.close();
    }

    @AfterClass
    public static void teardownPostprocessor() {
        if (postprocessor != null)
            postprocessor.close();
    }


    private static void test(String sourceText, String targetText, String alignText, String expected, String expectedNoTag) throws ProcessingException {
        test(sourceText, targetText, alignText, expected, expectedNoTag, language);
    }

    private static void test(String sourceText, String targetText, String alignText, String expected, String expectedNoTag, LanguageDirection language) throws ProcessingException {
        Sentence source = preprocessor.process(language, sourceText);
        Word[] targetWords = TokensOutputStream.deserializeWords(targetText);
        Alignment alignment = Alignment.fromAlignmentPairs(alignText);
        Translation translation = new Translation(targetWords, source, alignment);

        postprocessor.process(language,translation);

        assertEquals(expected, translation.toString());
        assertEquals(expectedNoTag, translation.toString(false, false));

        LanguageDirection reverseLanguage = new LanguageDirection(language.target, language.source);
        Sentence expectedSentence = preprocessor.process(reverseLanguage, expected);
    }


    @Test
    public void testStandardSpacingMonotone() throws Throwable {

        String sourceStr = "Hello, world!";
        String targetStr = "ciao , mondo !";
        String alignStr = "0-0 1-1 2-2 3-3";

        String expectedOutput = "Ciao, mondo!";
        String expectedOutputNoTag = "Ciao, mondo!";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag);
    }

    @Test
    public void testForcedSpacingMonotone() throws Throwable {

        String sourceStr = "Hello , world!";
        String targetStr = "ciao , mondo !";
        String alignStr = "0-0 1-1 2-2 3-3";

        String expectedOutput = "Ciao , mondo!";
        String expectedOutputNoTag = "Ciao , mondo!";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag);
    }

    @Test
    public void testOpeningNotEmptyMonotone() throws Throwable {

        String sourceStr = "Hello <b>world</b>!";
        String targetStr = "ciao mondo !";
        String alignStr = "0-0 1-1 2-2";

        String expectedOutput = "Ciao <b>mondo</b>!";
        String expectedOutputNoTag = "Ciao mondo!";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag);
    }

    @Test
    public void testOpeningNotEmptyMonotoneWithoutSpace() throws Throwable {

        String sourceStr = "Hello<b>world</b>!";
        String targetStr = "ciao mondo !";
        String alignStr = "0-0 1-1 2-2";

        String expectedOutput = "Ciao<b>mondo</b>!";
        String expectedOutputNoTag = "Ciao mondo!";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag);
    }


    @Test
    public void testFrenchEnglishRequiredSpaces() throws Throwable {

        String sourceStr = "A#A";
        String targetStr = "A # A";
        String alignStr = "0-0 1-1 2-2";

        String expectedOutput = "A#A";
        String expectedOutputNoTag = "A # A";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag, new LanguageDirection(Language.FRENCH, Language.ENGLISH));
    }


    @Test
    public void testFrenchEnglishWithoutTags() throws Throwable {

        String sourceStr = "A#A Par nature,";
        String targetStr = "A # A By nature ,";
        String alignStr = "0-0 1-1 2-2 3-3 4-3 4-4";

        String expectedOutput = "A#A By nature,";
        String expectedOutputNoTag = "A # A By nature,";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag, new LanguageDirection(Language.FRENCH, Language.ENGLISH));
    }

    @Test
    public void testFrenchEnglishWithTags() throws Throwable {

        String sourceStr = "<a>A#A</a> Par nature,";
        String targetStr = "A # A By nature ,";
        String alignStr = "0-0 1-1 2-2 3-3 4-3 4-4";

        String expectedOutput = "<a>A#A</a> By nature,";
        String expectedOutputNoTag = "A # A By nature,";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag, new LanguageDirection(Language.FRENCH, Language.ENGLISH));
    }




    @Test
    public void testFrenchEnglishWithComplexAlignment() throws Throwable {

        String sourceStr = "A#A <LbxTag001>Par<LbxTag002>nature,<LbxTag003> l' atteinte<LbxTag004> de<LbxTag005> ces<LbxTag006> objectifs est<LbxTag007> soumise<LbxTag008> à<LbxTag009> de<LbxTag010> nombreux<LbxTag011> risques<LbxTag012> et<LbxTag013> incertitudes<LbxTag014> susceptibles.<LbxTag015>";
        String targetStr = "A # A By nature , the achievement of these objectives is subject to many risks and uncertainties .";
        String alignStr = "0-0 1-1 2-2 3-3 4-3 4-4 5-8 7-3 7-5 8-6 9-6 10-10 11-11 12-12 13-11 14-13 15-14 16-15 17-16 17-17 18-17 19-18 19-17 20-18";

        String expectedOutput = "A#A <LbxTag001><LbxTag002><LbxTag003> By nature,<LbxTag004> <LbxTag005> the achievement of these<LbxTag006> objectives<LbxTag007> <LbxTag008> is subject<LbxTag009> to<LbxTag010> many<LbxTag011> risks<LbxTag012> and<LbxTag013> <LbxTag014> uncertainties.<LbxTag015>";
        String expectedOutputNoTag = "A # A By nature, the achievement of these objectives is subject to many risks and uncertainties.";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag, new LanguageDirection(Language.FRENCH, Language.ENGLISH));
    }

    @Test
    public void testEmbeddedTokensWithNonMonotoneAlignment() throws Throwable {

        String sourceStr = "example with <a>nested <b>tag</b></a>";
        String targetStr = "esempio con tag nidificato";
        String alignStr = "0-0 1-1 2-3 3-2";

        String expectedOutput = "esempio con <a> <b>tag</b> nidificato</a>";
        String expectedOutputNoTag = "esempio con tag nidificato";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag);
    }


    @Test
    public void testTagInsideWord() throws Throwable {

        String sourceStr = "C<a>lear cookies";
        String targetStr = "E limina i cookies";
        String alignStr = "0-0 1-1 2-2 2-3";

        String expectedOutput = "E<a>limina i cookies";
        String expectedOutputNoTag = "E limina i cookies";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag);
    }

    @Test
    public void testTagWithApostrophe() throws Throwable {

        String sourceStr = "The <a>URL</a>";
        String targetStr = "L' URL";
        String alignStr = "0-0 1-1";

        String expectedOutput = "L'<a>URL</a>";
        String expectedOutputNoTag = "L'URL";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag);
    }

    @Test
    public void testApostrophe() throws Throwable {

        String sourceStr = "The URL and the email";
        String targetStr = "L' URL e l' email";
        String alignStr = "0-0 1-1 2-2 3-3 4-4";

        String expectedOutput = "L'URL e l'email";
        String expectedOutputNoTag = "L'URL e l'email";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag);
    }

    @Test
    public void testApostropheAndTag() throws Throwable {

        String sourceStr = "The URL <a>and</a> <b>the email</b>";
        String targetStr = "L' URL e l' email";
        String alignStr = "0-0 1-1 2-2 3-3 4-4";

        String expectedOutput = "L'URL <a>e</a> <b>l'email</b>";
        String expectedOutputNoTag = "L'URL e l'email";

        test(sourceStr, targetStr, alignStr, expectedOutput, expectedOutputNoTag);
    }
}
