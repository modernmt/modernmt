package eu.modernmt.processing.tokenizer.opennlp;

import eu.modernmt.constants.Const;
import eu.modernmt.processing.framework.LanguageNotSupportedException;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.string.XMLEditableString;
import eu.modernmt.processing.tokenizer.Tokenizer;
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
public class OpenNLPTokenizer extends Tokenizer {

    private TokenizerME tokenizer;

    public OpenNLPTokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);

        File opennlp = new File(Const.fs.tokenizerModels, "opennlp");
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
    public XMLEditableString tokenize(XMLEditableString text, Map<String, Object> metadata) throws ProcessingException {
        return TokenizerOutputTransformer.transform(text, this.tokenizer.tokenize(text.toString()));
    }

}
