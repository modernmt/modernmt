package eu.modernmt.processing;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Word;
import eu.modernmt.model.XMLTag;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class PreprocessorTest {

    private static final LanguageDirection language = new LanguageDirection(Language.ENGLISH, Language.ITALIAN);

    private static Sentence process(String text) throws ProcessingException {
        return process(language, text);
    }

    private static Sentence process(LanguageDirection language, String text) throws ProcessingException {
        Preprocessor preprocessor = null;

        try {
            preprocessor = new Preprocessor();
            return preprocessor.process(language, text);
        } catch (IOException e) {
            throw new ProcessingException(e);
        } finally {
            IOUtils.closeQuietly(preprocessor);
        }
    }

    private static Word w(String text) {
        return new Word(text, text, null, null);
    }

    private static Word _w(String text) {
        return new Word(text, text, " ", null);
    }

    private static Word w_(String text) {
        return new Word(text, text, null, " ");
    }

    private static Word _w_(String text) {
        return new Word(text, text, " ", " ");
    }

    private static XMLTag t(String text, int pos) {
        return XMLTag.fromText(text, null, null, pos);
    }

    private static XMLTag _t(String text, int pos) {
        return XMLTag.fromText(text, " ", null, pos);
    }

    private static XMLTag t_(String text, int pos) {
        return XMLTag.fromText(text, null, " ", pos);
    }

    private static XMLTag _t_(String text, int pos) {
        return XMLTag.fromText(text, " ", " ", pos);
    }


    @Test
    public void testCommonSentence() throws ProcessingException {
        String text = "Hello world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
        assertFalse(sentence.hasTags());

        assertArrayEquals(new Word[]{
                w_("Hello"), _w("world"), w("!")
        }, sentence.getWords());
    }

    @Test
    public void testInitialTagWithSpace() throws ProcessingException {
        String text = "<a> Hello world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                _w_("Hello"), _w("world"), w("!")
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                t_("<a>", 0)
        }, sentence.getTags());
    }

    @Test
    public void testStrippedSentenceWithSpaceAfterTag() throws ProcessingException {
        String text = "Hello<a> world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                w("Hello"), _w("world"), w("!")
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                t_("<a>", 1)
        }, sentence.getTags());
    }

    @Test
    public void testStrippedSentenceWithSpacesBetweenTags() throws ProcessingException {
        String text = "Hello<a> <b>world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                w("Hello"), w("world"), w("!")
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                t_("<a>", 1), _t("<b>", 1)
        }, sentence.getTags());
    }

    @Test
    public void testRequiredSpaceTrue() throws ProcessingException {
        String text = "Hello<a>guys";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello guys", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                w("Hello"), w("guys")
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                t("<a>", 1)
        }, sentence.getTags());
    }

    @Test
    public void testRequiredSpaceFalse() throws ProcessingException {
        String text = "Hello<a>!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                w("Hello"), w("!")
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                t("<a>", 1)
        }, sentence.getTags());
    }

    @Test
    public void testRequiredSpaceFalseWithRightSpace() throws ProcessingException {
        String text = "Hello<a> !";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                w("Hello"), _w("!")
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                t_("<a>", 1)
        }, sentence.getTags());
    }

    @Test
    public void testRequiredSpaceFalseWithLeftSpace() throws ProcessingException {
        String text = "Hello <a>!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                w_("Hello"), w("!")
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                _t("<a>", 1)
        }, sentence.getTags());
    }

    @Test
    public void testTagAfterApostrophe() throws ProcessingException {
        String text = "Controlla l'<b>accesso</b> al <i>file</i>.";
        String strippedText = "Controlla l'accesso al file.";
        Sentence sentence = process(language.reversed(), text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals(strippedText, sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                w_("Controlla"), _w("l'"), w("accesso"), _w_("al"), w("file"), w(".")
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                t("<b>", 2), t_("</b>", 3), _t("<i>", 4), t("</i>", 5)
        }, sentence.getTags());
    }

    @Test
    public void testTagBeforeApostrophe() throws ProcessingException {
        String text = "Controlla l<b>'accesso</b> al <i>file</i>.";
        String correctedText = "Controlla l'<b>accesso</b> al <i>file</i>.";
        String strippedText = "Controlla l'accesso al file.";
        Sentence sentence = process(language.reversed(), text);

        assertEquals(correctedText, sentence.toString(true, false));
        assertEquals(strippedText, sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                w_("Controlla"), _w("l'"), w("accesso"), _w_("al"), w("file"), w(".")
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                t("<b>", 2), t_("</b>", 3), _t("<i>", 4), t("</i>", 5)
        }, sentence.getTags());
    }

    @Test
    public void testTagAroundApostrophe() throws ProcessingException {
        String text = "Controlla l<b>'</b>accesso al <i>file</i>.";
        String correctedText = "Controlla l'<b></b>accesso al <i>file</i>.";
        String strippedText = "Controlla l'accesso al file.";
        Sentence sentence = process(language.reversed(), text);

        assertEquals(correctedText, sentence.toString(true, false));
        assertEquals(strippedText, sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                w_("Controlla"), _w("l'"), w_("accesso"), _w_("al"), w("file"), w(".")
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                t("<b>", 2), t("</b>", 2), _t("<i>", 4), t("</i>", 5)
        }, sentence.getTags());
    }

}
