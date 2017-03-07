package eu.modernmt.processing.tokenizer.opennlp;

import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.TextProcessingModels;
import eu.modernmt.processing.ProcessingException;
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
 */
public class OpenNLPTokenizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    private TokenizerME tokenizer;

    public OpenNLPTokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);

        File opennlp = new File(TextProcessingModels.getPath(), "opennlp");
        File modelFile = new File(opennlp, sourceLanguage.getLanguage() + "-token.bin");

        if (!modelFile.isFile())
            throw new LanguageNotSupportedException(sourceLanguage);

        InputStream modelResource = null;

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

    @Override
    public SentenceBuilder call(SentenceBuilder text, Map<String, Object> metadata) throws ProcessingException {
        return TokenizerOutputTransformer.transform(text, this.tokenizer.tokenize(text.toString()));
    }

}
