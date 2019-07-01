package eu.modernmt.processing.normalizers;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ChineseCharacterConverter extends TextProcessor<Translation, Translation> {

    private static Map<Integer, Integer> loadDictionary(String filename) {
        HashMap<Integer, Integer> result = new HashMap<>();

        InputStream stream = null;
        LineIterator iterator = null;

        try {
            stream = ChineseCharacterConverter.class.getResourceAsStream(filename);
            iterator = IOUtils.lineIterator(stream, "UTF-8");
            while (iterator.hasNext()) {
                String line = iterator.nextLine();
                String[] keyValues = line.split("\t", 2);
                Integer key = keyValues[0].codePointAt(0);
                Integer value = keyValues[1].codePointAt(0);
                result.put(key, value);
            }

            return result;
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            IOUtils.closeQuietly(stream);
            if (iterator != null)
                iterator.close();
        }
    }

    private final Map<Integer, Integer> chars;

    public ChineseCharacterConverter(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        String language = targetLanguage.getLanguage();
        String region = targetLanguage.getRegion();

        if (!"zh".equals(language))
            throw new UnsupportedLanguageException(targetLanguage);

        if (region == null)
            chars = null;
        else if ("CN".equals(region))
            chars = loadDictionary("TSCharacters.txt");
        else if ("TW".equals(region))
            chars = loadDictionary("STCharacters.txt");
        else
            throw new UnsupportedLanguageException(targetLanguage);
    }

    @Override
    public Translation call(Translation param, Map<String, Object> metadata) throws ProcessingException {
        if (chars != null) {
            for (Word word : param.getWords()) {
                String text = convert(word.toString(false));
                if (text != null)
                    word.setText(text);
            }
        }

        return param;
    }

    private String convert(String text) {
        if (text.codePoints().noneMatch(chars::containsKey))
            return null;

        StringBuilder result = new StringBuilder(text.length());
        text.codePoints().forEach(code -> {
            Integer conversion = chars.get(code);
            if (conversion == null) {
                result.appendCodePoint(code);
            } else {
                result.appendCodePoint(conversion);
            }
        });

        return result.toString();
    }

}
