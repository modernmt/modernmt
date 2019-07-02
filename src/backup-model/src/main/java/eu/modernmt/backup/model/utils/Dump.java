package eu.modernmt.backup.model.utils;

import eu.modernmt.backup.model.BackupMemory;
import org.apache.commons.lang.StringUtils;

import java.io.File;

/**
 * Created by davide on 12/02/18.
 */
public class Dump {

    public static void main(String[] args) throws Throwable {
        if (args.length != 1)
            throw new IllegalArgumentException("Wrong number of arguments, usage: <model-path>");

        BackupMemory memory = new BackupMemory(new File(args[0]));
        memory.dump(entry -> {
            String str = StringUtils.join(new String[]{
                    Long.toString(entry.memory),
                    entry.language.source.toLanguageTag(),
                    entry.language.target.toLanguageTag(),
                    entry.sentence.replaceAll("\\s+", " "),
                    entry.translation.replaceAll("\\s+", " "),
            }, '\t');

            System.out.println(str);
        });
    }
}
