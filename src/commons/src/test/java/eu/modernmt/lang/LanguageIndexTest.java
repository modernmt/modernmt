package eu.modernmt.lang;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 10/02/18.
 */
public class LanguageIndexTest {

    private LanguageIndex index;

    private static LanguagePair lp(String string) {
        String[] parts = string.split("\\s+");
        return new LanguagePair(Language.fromString(parts[0]), Language.fromString(parts[1]));
    }

    private static List<LanguagePair> list(String string) {
        ArrayList<LanguagePair> result = new ArrayList<>();
        for (String part : string.split(","))
            result.add(lp(part.trim()));
        return result;
    }

    @Before
    public void setup() {
        index = new LanguageIndex(
                lp("en fr"),
                lp("zh-TW en"),
                lp("en zh-TW"),
                lp("zh-CN en"),
                lp("en zh-CN"),
                lp("zh-CN pt-PT"),
                lp("zh-CN pt-BR"),
                lp("zh-TW pt-BR"));
    }

    @Test
    public void testMap() {
        assertEquals(list("en fr"), index.map(lp("en-GB fr")));
        assertEquals(list("en fr"), index.map(lp("en fr")));
        assertEquals(list("fr en"), index.map(lp("fr en")));
        assertEquals(list("zh-TW en, zh-CN en"), index.map(lp("zh en-GB")));
        assertEquals(list("en zh-TW, en zh-CN"), index.map(lp("en zh")));
        assertTrue(index.map(lp("zh-TW pt-PT")).isEmpty());
        assertEquals(list("zh-CN pt-PT, zh-CN pt-BR"), index.map(lp("zh-CN pt")));
    }
}
