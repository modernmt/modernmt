package eu.modernmt.cleaning.normalizers;

import eu.modernmt.cleaning.Normalizer;
import eu.modernmt.model.Tag;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.util.regex.Matcher;

/**
 * Created by davide on 17/11/16.
 */
public class XMLStripper implements Normalizer {

    private static String stripXML(String line) {
        StringBuilder builder = new StringBuilder();
        Matcher m = Tag.TagRegex.matcher(line);

        int stringIndex = 0;
        char[] chars = line.toCharArray();

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            if (stringIndex < start)
                builder.append(chars, stringIndex, start - stringIndex);

            stringIndex = end;

            builder.append(' ');
        }

        if (stringIndex < chars.length)
            builder.append(chars, stringIndex, chars.length - stringIndex);

        return builder.toString();
    }

    @Override
    public void normalize(MultilingualCorpus.StringPair pair, int index) {
        pair.source = stripXML(pair.source).trim();
        pair.target = stripXML(pair.target).trim();
    }

}
