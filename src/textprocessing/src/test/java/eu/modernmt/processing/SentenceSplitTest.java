package eu.modernmt.processing;

import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.*;
import eu.modernmt.processing.splitter.SentenceSplitter;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class SentenceSplitTest {

    private static final LanguageDirection language = new LanguageDirection(Language.ENGLISH, Language.ITALIAN);

    private static Sentence process(String text, boolean splitByNewline) throws ProcessingException {
        Preprocessor.Options options = new Preprocessor.Options();
        options.splitByNewline = splitByNewline;

        Preprocessor preprocessor = null;

        try {
            preprocessor = new Preprocessor();
            return preprocessor.process(language, text, options);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        } finally {
            IOUtils.closeQuietly(preprocessor);
        }
    }

    @Test
    public void testSplitNoNewline() throws ProcessingException {
        String text = "This is an example sentence. And this is a list:\n- list item one\n- list item two";
        Sentence sentence = process(text, false);

        List<Sentence> sentences = SentenceSplitter.split(sentence, true);

        assertEquals(2, sentences.size());
        assertEquals("This is an example sentence. ", sentences.get(0).toString());
        assertEquals("And this is a list:\n- list item one\n- list item two", sentences.get(1).toString());
    }

    @Test
    public void testSplitWithNewline() throws ProcessingException {
        String text = "This is an example sentence. And this is a list:\n- list item one\n- list item two";
        Sentence sentence = process(text, true);

        List<Sentence> sentences = SentenceSplitter.split(sentence, true);

        assertEquals(4, sentences.size());
        assertEquals("This is an example sentence. ", sentences.get(0).toString());
        assertEquals("And this is a list:\n", sentences.get(1).toString());
        assertEquals("- list item one\n", sentences.get(2).toString());
        assertEquals("- list item two", sentences.get(3).toString());
    }

}
