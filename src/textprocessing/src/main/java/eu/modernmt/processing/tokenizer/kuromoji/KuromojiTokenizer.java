package eu.modernmt.processing.tokenizer.kuromoji;

import com.atilika.kuromoji.ipadic.Token;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;

import java.util.List;
import java.util.Map;

/**
 * Created by davide on 11/11/15.
 * Updated by andrearossi on 01/03/2017
 * <p>
 * A KuromojiTokenizer is an object that performs word tokenization of a string
 * based on the Kuromoji tokenization and analysis library.
 * Unlike other libraries, Kuromoji only handles one source language: Japanese.
 * <p>
 * The KuromojiTokenizer has knowledge of class com.atilika.kuromoji.ipadic.Tokenizer
 * that Kuromoji employs for tokenizing Japanese texts
 */
public class KuromojiTokenizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    /*create a new Kuromoji Library Tokenizer*/
    private com.atilika.kuromoji.ipadic.Tokenizer tokenizer = new com.atilika.kuromoji.ipadic.Tokenizer();

    /**
     * This constructor builds a KuromojiTokenizer for tokenizing Japanese texts.
     * The KuromojiTokenizer is initialized by setting source and target language
     * (throwing an exception if the source one isn't Japanese),
     * and by instantiating the Kuromoji Library Tokenizer that this
     * KuromojiTokenizer will need to employ.
     *
     * @param sourceLanguage the initial language of the text to translate
     * @param targetLanguage the language that the test must be translated to
     * @throws UnsupportedLanguageException if the sourceLanguage is not Japanese
     */
    public KuromojiTokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
        if (!Language.JAPANESE.getLanguage().equals(sourceLanguage.getLanguage()))
            throw new UnsupportedLanguageException(sourceLanguage);
    }

    /**
     * This method uses the Kuromoji Library Tokenizer object
     * to perform word tokenization of the current string in the SentenceBuilder.
     * <p>
     * It extracts the current string to process from the builder
     * and pass it to the tokenizer, obtaining a list of
     * Kuromoji Library Token objects.
     * <p>
     * For each Kuromoji Library Token, if its text is empty
     * the token is considered not relevant (whitespace) so it is filtered out.
     * <p>
     * In the end for each Kuromoji Library Token, the text string
     * is extracted and put inside an array, that is passed to the
     * TokenizerUtils static object
     * so that it can transform each token String into an actual WORD Token.*
     *
     * @param builder  the SentenceBuilder that holds the current string to tokenize
     * @param metadata additional information on the current pipe
     *                 (not used in this specific operation)
     * @return the SentenceBuilder received as a parameter;
     * its internal state has been updated by the execution of the call() method
     * @throws ProcessingException
     */
    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) throws ProcessingException {
        List<Token> tokens = tokenizer.tokenize(builder.toString());
        // Remove empty tokens
        tokens.removeIf(token -> token.getSurface().trim().length() == 0);

        SentenceBuilder.Editor editor = builder.edit();
        for (Token token : tokens)
            editor.setWord(token.getPosition(), token.getSurface().length(), null);

        return editor.commit();
    }

}
