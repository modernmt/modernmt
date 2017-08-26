package eu.modernmt.cleaning.filters.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by davide on 26/08/17.
 */
public class WordCounter {

    private static final Pattern DELIMITERS_REGEX = Pattern.compile("[\\s\\p{Punct}\\u00A0]+");
    private static final Pattern CJK_REGEX = Pattern.compile("[\\p{IsHan}\\p{IsKatakana}\\p{IsHiragana}]");

    /**
     * Count the amount of words in a corpus line
     *
     * @param line     the corpus line to count the lines of which
     * @param language the language of the corpus line
     * @return the words count
     */
    public static int count(String line, Locale language) {
        int wordCount = 0;

        Matcher delimitersMatcher = DELIMITERS_REGEX.matcher(line);

        int wordStart = 0;

        while (delimitersMatcher.find()) {
            int wordEnd = delimitersMatcher.start();

            if (wordStart == wordEnd)
                continue;
            wordCount += countWordsInToken(line.substring(wordStart, wordEnd));
            wordStart = delimitersMatcher.end();
        }

        if (wordStart != line.length()) {
            wordCount += countWordsInToken(line.substring(wordStart, line.length()));
        }

        return wordCount;
    }


    /**
     * this private method counts the amount of words
     * in a token delimited by ponctuation symbols,
     * considering each CJK character as a separate word
     *
     * @param token the amount of words
     * @return the amount of words in token
     */
    private static int countWordsInToken(String token) {
        int wordCount = 0;

        //start index of previous CJK in token
        int prev = -1;
        //start index of current CJK in token
        int cur = 0;

        Matcher cjkMatcher = CJK_REGEX.matcher(token);
        while (cjkMatcher.find()) {
            // if the matcher finds a CJK character, it is a separate word
            // so increment the word count
            wordCount++;

            // moreover, if the previous CJK is more distant than 1 from the current one,
            // it means there is another word in between
            // so increment the word count
            cur = cjkMatcher.start();
            if (cur > prev + 1)
                wordCount++;
            prev = cur;
        }

        // if the last CJK character is not the last character,
        // it means that there is one last word to count
        // so increment the word count
        if (prev < token.length() - 1)
            wordCount++;

        return wordCount;
    }
}
