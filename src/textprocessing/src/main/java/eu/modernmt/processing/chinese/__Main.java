package eu.modernmt.processing.chinese;

import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.processing.Preprocessor;
import org.apache.commons.io.IOUtils;

public class __Main {

    public static void main(String[] args) throws Throwable {
        LanguagePair language = new LanguagePair(Language.CHINESE, Language.ENGLISH);

        Preprocessor preprocessor = new Preprocessor();
        try {
            String tokenized = TokensOutputStream.serialize(preprocessor.process(language, "㓆\uD841\uDDE3\uD841\uDDE3㓆."), false, true);
            String tokenized2 = TokensOutputStream.serialize(preprocessor.process(language, "㓆af."), false, true);
            System.out.println(tokenized);
            System.out.println(tokenized2);
        } finally {
            IOUtils.closeQuietly(preprocessor);
        }
    }
}
