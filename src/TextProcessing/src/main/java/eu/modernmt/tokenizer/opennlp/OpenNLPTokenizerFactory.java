package eu.modernmt.tokenizer.opennlp;

import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;
import opennlp.tools.tokenize.TokenizerModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by davide on 13/11/15.
 */
public class OpenNLPTokenizerFactory extends ITokenizerFactory {

    private String languageCode;

    public OpenNLPTokenizerFactory(String languageCode) {
        this.languageCode = languageCode;
    }

    @Override
    public ITokenizer newInstance() {
        File opennlp = new File(ITokenizer.MODELS_PATH, "opennlp");

        File modelFile = new File(opennlp, this.languageCode + "-token.bin");

        if (!modelFile.isFile())
            throw new IllegalArgumentException("Unsupported language: " + this.languageCode);

        InputStream modelResource = null;

        try {
            modelResource = new FileInputStream(modelFile);
            TokenizerModel model = new TokenizerModel(modelResource);
            return new OpenNLPTokenizer(model);
        } catch (IOException e) {
            throw new RuntimeException("Unable to newInstance model from resource", e);
        } finally {
            if (modelResource != null)
                try {
                    modelResource.close();
                } catch (IOException e) {
                }
        }
    }
}
