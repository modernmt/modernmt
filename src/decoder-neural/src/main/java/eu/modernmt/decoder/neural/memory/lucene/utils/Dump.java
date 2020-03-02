package eu.modernmt.decoder.neural.memory.lucene.utils;

import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import org.apache.commons.lang.StringUtils;

import java.io.File;

/**
 * Created by davide on 12/02/18.
 */
public class Dump {

    public static void main(String[] args) throws Throwable {
        if (args.length != 1)
            throw new IllegalArgumentException("Wrong number of arguments, usage: <model-path>");

        LuceneTranslationMemory memory = new LuceneTranslationMemory(new File(args[0]), 1);
        memory.dumpAll(entry -> {
            String str = StringUtils.join(new String[]{
                    Long.toString(entry.memory),
                    entry.language.source.toLanguageTag(),
                    entry.language.target.toLanguageTag(),
                    entry.sentence.replace('\t', ' '),
                    entry.translation.replace('\t', ' ')
            }, '\t');

            System.out.println(str);
        });
    }
}
