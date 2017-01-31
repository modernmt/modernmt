package eu.modernmt.processing;

import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.xmessage.XMessageWordTransformer;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 04/04/16.
 */
public class WordTransformationFactory extends TextProcessor<Translation, Translation> {

    public interface WordTransformer {

        boolean match(Word word);

        Word setupTransformation(Word word);

    }

    public WordTransformationFactory(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    private static final List<Class<? extends WordTransformer>> TRANSFORMERS = Collections.singletonList(XMessageWordTransformer.class);

    private static WordTransformer[] instantiate(List<Class<? extends WordTransformer>> transformerList) {
        WordTransformer[] instances = new WordTransformer[transformerList.size()];

        int i = 0;
        for (Class<? extends WordTransformer> cls : transformerList) {
            try {
                instances[i++] = cls.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new Error("Error while creating WordTransformer instance: " + cls.getName(), e);
            }
        }

        return instances;
    }

    private static Word apply(WordTransformer[] transformers, Word word) {
        for (int i = transformers.length - 1; i >= 0; i--) {
            WordTransformer transformer = transformers[i];

            if (transformer.match(word))
                return transformer.setupTransformation(word);
        }

        return null;
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) {
        WordTransformer[] transformers = instantiate(TRANSFORMERS);

        Word[] words = translation.getWords();
        for (int i = 0; i < words.length; i++) {
            Word word = apply(transformers, words[i]);

            if (word != null)
                words[i] = word;
        }

        return translation;
    }

}
