package eu.modernmt.processing.tokenizer.opennlp;

import eu.modernmt.lang.Language2;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.TextProcessingModels;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.TokenizedString;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OpenNLPTokenAnnotator implements BaseTokenizer.Annotator {

    private final TokenizerME tokenizer;

    public static OpenNLPTokenAnnotator forLanguage(Language2 language) throws UnsupportedLanguageException {
        File opennlp = new File(TextProcessingModels.getPath(), "opennlp");
        File modelFile = new File(opennlp, language.getLanguage() + "-token.bin");

        /*if there is no file on the path specified by modelFile,
         * it means that the sourceLanguage is not supported*/
        if (!modelFile.isFile())
            throw new UnsupportedLanguageException(language);

        InputStream modelResource = null;

        /*try to open the language model file
         * and to use it to create a tokenizer*/
        try {
            modelResource = new FileInputStream(modelFile);
            TokenizerModel model = new TokenizerModel(modelResource);
            return new OpenNLPTokenAnnotator(new TokenizerME(model));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load OpenNLP model at " + modelFile, e);
        } finally {
            IOUtils.closeQuietly(modelResource);
        }
    }

    private OpenNLPTokenAnnotator(TokenizerME tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public void annotate(TokenizedString string) {
        Span[] tokens = this.tokenizer.tokenizePos(string.toString());

        for (Span token : tokens) {
            int start = token.getStart();
            int end = token.getEnd();
            int length = end - start;

            string.setWord(start, start + length);
        }
    }

}
