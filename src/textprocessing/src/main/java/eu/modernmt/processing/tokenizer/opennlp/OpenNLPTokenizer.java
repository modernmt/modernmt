package eu.modernmt.processing.tokenizer.opennlp;

import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessingModels;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 27/01/16.
 * Updated by andrearossi on 01/03/2017.
 * <p>
 * An OpenNLPTokenizer has the responsibility
 * to handle tokenizers for the languages in use
 * and to employ them to tokenize the string under analysis
 */
public class OpenNLPTokenizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    private TokenizerME tokenizer;

    /**
     * This constructor builds an OpenNLPTokenizer for the language couple under analysis;
     * it reads from a file the language model for the source language,
     * then
     *
     * @param sourceLanguage the initial language of the string that must be translated
     * @param targetLanguage the language that string must be translated to
     * @throws LanguageNotSupportedException if a not supported language is involved
     */
    public OpenNLPTokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);

        /*new file for...*/
        File opennlp = new File(TextProcessingModels.getPath(), "opennlp");
        /*The File object allows to read the language model file for the source language*/
        File modelFile = new File(opennlp, sourceLanguage.getLanguage() + "-token.bin");

                /*if there is no file on the path specified by modelFile,
        * it means that the sourceLanguage is not supported*/
        if (!modelFile.isFile())
            throw new LanguageNotSupportedException(sourceLanguage);

        InputStream modelResource = null;

        /*try to open the language model file
        * and to use it to create a tokenizer*/
        try {
            modelResource = new FileInputStream(modelFile);
            TokenizerModel model = new TokenizerModel(modelResource);
            this.tokenizer = new TokenizerME(model);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load OpenNLP model at " + modelFile, e);
        } finally {
            IOUtils.closeQuietly(modelResource);
        }
    }

    /**
     * This method uses the TokenizerME object for the current source language
     * to perform word tokenization of the current string in the SentenceBuilder.
     * <p>
     * The resulting the token Strings array is passed to the
     * TokenizerOutputTransformer to transform each token String
     * into an actual WORD Token.*
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
        return TokenizerOutputTransformer.transform(builder, this.tokenizer.tokenize(builder.toString()));
    }

}
