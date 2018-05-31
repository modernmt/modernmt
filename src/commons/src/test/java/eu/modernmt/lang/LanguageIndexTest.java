package eu.modernmt.lang;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by davide on 10/02/18.
 */
public class LanguageIndexTest {

    private LanguageIndex index;

    private static Language l(String s) {
        return Language.fromString(s);
    }

    private static LanguagePair lp(String s) {
        String[] parts = s.split("\\s+");
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
        index = new LanguageIndex.Builder()
                .add(lp("en it"))
                .add(lp("en fr"))
                .add(lp("en es-ES"))
                .add(lp("en es-MX"))
                .add(lp("en zh-CN"))
                .add(lp("en zh-TW"))

                .addRule(l("es"), l("es"), l("es-ES"))
                .addWildcardRule(l("es"), l("es-MX"))
                .addRule(l("zh"), l("zh-HK"), l("zh-TW"))
                .addWildcardRule(l("zh"), l("zh-CN"))

                .build();
    }

    @Test
    public void testMap() {
        //en it
        assertEquals(lp("en it"), index.map(lp("en it")));
        assertNull(index.map(lp("it en")));
        assertNull(index.map(lp("it-IT en")));
        assertNull(index.map(lp("it en-US")));
        assertNull(index.map(lp("it-IT en-US")));

        // en fr
        assertEquals(lp("en fr"), index.map(lp("en-GB fr-FR")));
        assertEquals(lp("en fr"), index.map(lp("en-GB fr")));
        assertEquals(lp("en fr"), index.map(lp("en fr-FR")));
        assertEquals(lp("en fr"), index.map(lp("en fr")));
        assertNull(index.map(lp("fr en")));

        // en es
        assertEquals(lp("en es-ES"), index.map(lp("en es")));
        assertEquals(lp("en es-ES"), index.map(lp("en es-ES")));
        assertEquals(lp("en es-MX"), index.map(lp("en es-MX")));
        assertEquals(lp("en es-MX"), index.map(lp("en es-CO")));
        assertEquals(lp("en es-MX"), index.map(lp("en-US es-CO")));
        assertNull(index.map(lp("es en")));

        // en zh
        assertEquals(lp("en zh-CN"), index.map(lp("en zh")));
        assertEquals(lp("en zh-CN"), index.map(lp("en zh-CN")));
        assertEquals(lp("en zh-CN"), index.map(lp("en zh-XX")));
        assertEquals(lp("en zh-TW"), index.map(lp("en zh-TW")));
        assertEquals(lp("en zh-TW"), index.map(lp("en zh-HK")));
        assertNull(index.map(lp("zh en")));

        // reversed
        assertEquals(lp("en fr"), index.map(lp("fr-FR en-US").reversed()));
        assertEquals(lp("en es-ES"), index.map(lp("es en-US").reversed()));
        assertEquals(lp("en zh-CN"), index.map(lp("zh en").reversed()));
    }
}
