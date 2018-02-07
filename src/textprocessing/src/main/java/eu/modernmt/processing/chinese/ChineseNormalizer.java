package eu.modernmt.processing.chinese;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nicola on 06/02/18
 * <p>
 * A ChineseNormalizer has the responsibility
 * to transform any Chinese variants (Simplified, Tranditional
 * (including Taiwan and Hong Kong standard) into Simplified Chinese
 */
public class ChineseNormalizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    private ChineseDetector detector;
    private Map<String, ChineseConverter> converters;
    private Language internalLanguage;

    /**
     * This constructor builds a ChineseNormalizer
     * based on the initial language and the destination language of the translation process.
     *
     * @param sourceLanguage the language of the input String
     * @param targetLanguage the language the input String must be translated to
     * @throws UnsupportedLanguageException the requested language is not supported by this software
     */
    public ChineseNormalizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
        internalLanguage = new Language("zh",null,"TW"); //by default, we use Traditional Chinese (zh-TW) internally

        converters = new HashMap<>();
        detector = new ChineseDetector();

        if ( ! detector.support(sourceLanguage) ) {
            throw new UnsupportedLanguageException(sourceLanguage);
        }
    }

    /**
     * Method that, given a SentenceBuilder with the string to process,
     * extracts the string, scans it looking for whitespace characters sequences
     * and requests either their deletion,
     * if they are at the very beginning or at the very end end of the string
     * or their their replacement with a blank space (" ") in any other case.
     *
     * @param inputText  a SentenceBuilder that holds the input String
     *                 and can generate Editors to process it
     * @param metadata additional information on the current pipe
     *                 (not used in this specific operation)
     * @return the SentenceBuilder received as a parameter;
     * its internal state has been updated by the execution of the call() method
     * @throws ProcessingException
     */
    @Override
    public SentenceBuilder call(SentenceBuilder inputText, Map<String, Object> metadata) throws ProcessingException {

        String sLanguage = detector.detect(inputText.toString());
        if (sLanguage.equals(internalLanguage.getRegion())) {
            return inputText;
        } else {
            String conversion = sLanguage + "-" + internalLanguage.getRegion();
            if (!converters.containsKey(conversion)) {
                try {
                    converters.put(conversion, new ChineseConverter(conversion));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            ChineseConverter converter = converters.get(conversion);
            String convertedText = converter.convert(inputText.toString());
            return new SentenceBuilder(convertedText);
        }
    }

    public static void main(String[] args) throws ProcessingException {
        Language src = new Language("zh",null,null);
        Language trg = new Language("en");

        long startTime = System.nanoTime();
        System.err.println("start at " + startTime);
        ChineseNormalizer n = new ChineseNormalizer(src, trg);
        long endTime = System.nanoTime();
        long l = (endTime - startTime)/1000000;
        System.err.println("end at " + endTime);
        System.err.println("start up of normalizer; duration l: " + l + " milliseconds");


//        String from = "开放中文转换开放中文转换开放中文转换开放中文转换开放中文转换";
        String from = "开 放 中 文 转 换 开 放 中 文 转 换 开 放 中 文 转 换 开 放 中 文 转 换 开 放 中 文 转 换";
        SentenceBuilder builder = new SentenceBuilder(from);
        Map<String, Object> metadata = new HashMap<>();


        String to = "";
        startTime = System.nanoTime();
        to = n.call(builder, metadata).toString();
        endTime = System.nanoTime();
        l = (endTime - startTime)/1000000;
        System.err.println("start up of converter; duration l: " + l + " milliseconds");

        System.err.println("from:" + from);
        System.err.println("to:" + to);

        startTime = System.nanoTime();

        int N=10000;
        for (int i=0; i < N; ++i) {
            to = n.call(builder, metadata).toString();
        }
        endTime = System.nanoTime();
        l = (endTime - startTime)/1000000;
        System.err.println("from:" + from);
        System.err.println("to:" + to);
        System.err.println("duration l: " + l + " milliseconds");
        System.err.println("speed: " + ((float) l)/N + " milliseconds/sentence");

    }
}