package eu.modernmt.processing.chinese;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Sentence;
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
public class ChineseDeNormalizer extends TextProcessor<Translation, Translation> {

    private ChineseDetector detector;
    private Map<String, ChineseConverter> converters;
    private Language internalLanguage;

    /**
     * This constructor builds a ChineseDeNormalizer
     * based on the initial language and the destination language of the translation process.
     *
     * @param sourceLanguage the language of the input String
     * @param targetLanguage the language the input String must be translated to
     * @throws UnsupportedLanguageException the requested language is not supported by this software
     */
    public ChineseDeNormalizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
        internalLanguage = new Language("zh",null,"TW"); //by default, we use Traditional Chinese (zh-TW) internally

        converters = new HashMap<>();
        detector = new ChineseDetector();

        if ( ! detector.support(sourceLanguage) ) {
            throw new UnsupportedLanguageException(sourceLanguage);
        }
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        String inputText = translation.toString();
        String tLanguage = detector.detect(inputText);

        if (tLanguage.equals(internalLanguage.getRegion())) {
            return translation;
        }

        String conversion = internalLanguage.getRegion() + "-" + tLanguage;
        if (!converters.containsKey(conversion)) {
            try {
                converters.put(conversion, new ChineseConverter(conversion));
            } catch (IOException e) {
                e.printStackTrace();
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
            String c = converter.convert(inputWords[i].toString());
            convertedWords[i] = new Word(converter.convert(inputWords[i].toString()));
            System.err.println("from:" + inputWords[i].toString() + " " + " to:" + c );
        }

        Translation convertedTranslation = new Translation(convertedWords, translation.getTags(), translation.getSource(), translation.getWordAlignment());
        return convertedTranslation;
    }


    public static void main(String[] args) throws ProcessingException {
        Language src = new Language("zh",null,"CN");
        Language trg = new Language("en");

        long startTime = System.nanoTime();
        System.err.println("start at " + startTime);
        ChineseDeNormalizer n = new ChineseDeNormalizer(src, trg);
        long endTime = System.nanoTime();
        long l = (endTime - startTime)/1000000;
        System.err.println("end at " + endTime);
        System.err.println("start up of normalizer; duration l: " + l + " milliseconds");


//        String from = "开放中文转换开放中文转换开放中文转换开放中文转换开放中文转换";
        Sentence source = new Sentence(null);
//        String from = "开 放 中 文 转 换 开 放 中 文 转 换 开 放 中 文 转 换 开 放 中 文 转 换 开 放 中 文 转 换";
        String from = "㑶 㑮   開 放 中 文 轉 換 開 放 中 文 轉 換 開 放 中 文 轉 換 開 放 中 文 轉 換 開 放 中 文 轉 換";


        String[] fromString = from.split(" ");
        Word[] fromWords = new Word[fromString.length];
        for (int i = 0; i < fromString.length; ++i) {
            fromWords[i] = new Word(fromString[i]);
        }

        Translation translation = new Translation(fromWords, source, null);
        Map<String, Object> metadata = new HashMap<>();


        String to = "";
        startTime = System.nanoTime();
        to = n.call(translation, metadata).toString();
        endTime = System.nanoTime();
        l = (endTime - startTime)/1000000;
        System.err.println("start up of converter; duration l: " + l + " milliseconds");

        System.err.println("from:" + from);
        System.err.println("to:" + to);

        startTime = System.nanoTime();

        int N=100;
        for (int i=0; i < N; ++i) {
            to = n.call(translation, metadata).toString();
        }
        endTime = System.nanoTime();
        l = (endTime - startTime)/1000000;
        System.err.println("from:" + from);
        System.err.println("to:" + to);
        System.err.println("duration l: " + l + " milliseconds");
        System.err.println("speed: " + ((float) l)/N + " milliseconds/sentence");

    }
}
