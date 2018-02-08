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

    //    @Override
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

//    public static void main(String[] args) throws ProcessingException {
//        Language src = new Language("en");
//        Language trg = new Language("zh","CN");
//
//        long startTime = System.nanoTime();
//        ChinesePostprocessor normalizer = new ChinesePostprocessor(src, trg);
//        long l = (System.nanoTime() - startTime)/1000000;
//        System.out.println("start up of normalizer; duration l: " + l + " milliseconds");
//
//        String from;
//        from = "閈 开 開 开 開 放 中 文 转 换 㑶㑮開放 中文轉換開 放中文轉換開放";
//
//        String[] fromString = from.split(" ");
//        Word[] fromWords = new Word[fromString.length];
//        for (int i = 0; i < fromString.length; ++i) {
//            fromWords[i] = new Word(fromString[i]);
//        }
//
//        Translation translation = new Translation(fromWords, new Sentence(fromWords), null);
//        Map<String, Object> metadata = new HashMap<>();
//
//        startTime = System.nanoTime();
//        Translation to = normalizer.call(translation, metadata);
//        l = (System.nanoTime() - startTime)/1000000;
//        System.out.println("start up of converter; duration l: " + l + " milliseconds");
//        System.out.println("from:" + from + " ==>  to :" + to);
//
//        startTime = System.nanoTime();
//        int N=10000;
//        for (int i=0; i < N; ++i) { to = normalizer.call(translation, metadata); }
//        l = (System.nanoTime() - startTime)/1000000;
//        System.out.println("from:" + from + " ==>  to :" + to);
//        System.out.println("duration l: " + l + " milliseconds");
//        System.out.println("speed: " + ((float) l)/N + " milliseconds/sentence");
//
//    }
}
