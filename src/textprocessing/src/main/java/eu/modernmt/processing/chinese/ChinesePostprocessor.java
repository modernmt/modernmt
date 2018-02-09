package eu.modernmt.processing.chinese;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by davide on 08/04/16.
 */
public class ChinesePostprocessor extends TextProcessor<Translation, Translation> {
    private static final ChineseDetector detector = new ChineseDetector();
    private static final Map<String, ChineseConverter> converters = new HashMap<>();
    private static Language targetLanguage, internalLanguage;

    /**
     * This constructor builds a ChinesePostprocessor
     * based on the initial language and the destination language of the translation process.
     *
     * @param sourceLanguage the language of the input String
     * @param targetLanguage the language the input String must be translated to
     * @throws UnsupportedLanguageException the requested language is not supported by this software
     */
    public ChinesePostprocessor(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
        this.targetLanguage = targetLanguage;
        internalLanguage = new Language("zh",null,"TW"); //by default, we use Traditional Chinese (zh-TW) internally

        if ( ! detector.support(this.targetLanguage) ) {
            throw new UnsupportedLanguageException(sourceLanguage);
        }
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        if (targetLanguage.equals(internalLanguage)) {
            return translation;
        }

        String conversion = internalLanguage.getRegion() + "-" + targetLanguage.getRegion();
        if (!converters.containsKey(conversion)) {
            try {
                converters.put(conversion, new ChineseConverter(internalLanguage,targetLanguage));
            } catch (IOException e) {
                throw new Error(e);
            }
        }
        ChineseConverter converter = converters.get(conversion);

        //convert best translation
        Translation convertedTranslation = convert(converter,translation);

        //convert all nbest
        if (translation.hasNbest()) {
            List<Translation> convertedNbest = null;
            for (Translation tr : translation.getNbest()) {
                convertedNbest.add(convert(converter, tr));
            }
            convertedTranslation.setNbest(convertedNbest);
        }
        return convertedTranslation;
    }

    public Translation call_with_detection(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        String conversion = internalLanguage.getRegion() + "-" + targetLanguage.getRegion();
        if (!converters.containsKey(conversion)) {
            try {
                converters.put(conversion, new ChineseConverter(internalLanguage, targetLanguage));
            } catch (IOException e) {
                throw new Error(e);
            }
        }
        ChineseConverter converter = converters.get(conversion);

        //convert best translation
        Translation convertedTranslation = convert(converter,translation);

        //convert all nbest
        if (translation.hasNbest()) {
            List<Translation> convertedNbest = null;
            for (Translation tr : translation.getNbest()) {
                convertedNbest.add(convert(converter, tr));
            }
            convertedTranslation.setNbest(convertedNbest);
        }
        return convertedTranslation;
    }

    private Translation convert(ChineseConverter converter, Translation translation){
        Word[] inputWords  = translation.getWords();
        Word[] convertedWords = new Word[inputWords.length];
        for (int i = 0; i < inputWords.length; ++i) {
            convertedWords[i] = new Word(converter.convert(inputWords[i].toString()));
        }

        return new Translation(convertedWords, translation.getTags(), translation.getSource(), translation.getWordAlignment());
    }
}
