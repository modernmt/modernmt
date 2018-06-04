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
                .add(lp("fr en"))
                .add(lp("en es-ES"))
                .add(lp("en es-MX"))
                .add(lp("en zh-CN"))
                .add(lp("en zh-TW"))
                .add(lp("zh en"))

                .addRule(l("es"), l("es"), l("es-ES"))
                .addWildcardRule(l("es"), l("es-MX"))
                .addRule(l("zh"), l("zh-HK"), l("zh-TW"))
                .addWildcardRule(l("zh"), l("zh-CN"))

                .build();
    }

    @Test
    public void simpleLanguageMonodirectional() {
        assertEquals(lp("en it"), index.map(lp("en it"), true));
        assertEquals(lp("en it"), index.map(lp("en it-IT"), true));
        assertEquals(lp("en it"), index.map(lp("en-US it"), true));
        assertEquals(lp("en it"), index.map(lp("en-US it-IT"), true));

        assertNull(index.map(lp("it en"), true));
        assertNull(index.map(lp("it-IT en"), true));
        assertNull(index.map(lp("it en-US"), true));
        assertNull(index.map(lp("it-IT en-US"), true));

        assertEquals(lp("en it"), index.map(lp("en it"), false));
        assertEquals(lp("en it"), index.map(lp("en it-IT"), false));
        assertEquals(lp("en it"), index.map(lp("en-US it"), false));
        assertEquals(lp("en it"), index.map(lp("en-US it-IT"), false));

        assertNull(index.map(lp("it en"), false));
        assertNull(index.map(lp("it-IT en"), false));
        assertNull(index.map(lp("it en-US"), false));
        assertNull(index.map(lp("it-IT en-US"), false));
    }

    @Test
    public void simpleLanguageBidirectional() {
        assertEquals(lp("en fr"), index.map(lp("en fr"), true));
        assertEquals(lp("en fr"), index.map(lp("en fr-FR"), true));
        assertEquals(lp("en fr"), index.map(lp("en-GB fr"), true));
        assertEquals(lp("en fr"), index.map(lp("en-GB fr-FR"), true));

        assertEquals(lp("fr en"), index.map(lp("fr en"), true));
        assertEquals(lp("fr en"), index.map(lp("fr-FR en"), true));
        assertEquals(lp("fr en"), index.map(lp("fr en-GB"), true));
        assertEquals(lp("fr en"), index.map(lp("fr-FR en-GB"), true));

        assertEquals(lp("en fr"), index.map(lp("en fr"), false));
        assertEquals(lp("en fr"), index.map(lp("en fr-FR"), false));
        assertEquals(lp("en fr"), index.map(lp("en-GB fr"), false));
        assertEquals(lp("en fr"), index.map(lp("en-GB fr-FR"), false));

        assertEquals(lp("fr en"), index.map(lp("fr en"), false));
        assertEquals(lp("fr en"), index.map(lp("fr-FR en"), false));
        assertEquals(lp("fr en"), index.map(lp("fr en-GB"), false));
        assertEquals(lp("fr en"), index.map(lp("fr-FR en-GB"), false));
    }

    @Test
    public void ruledLanguageMonodirectional() {
        assertEquals(lp("en es-ES"), index.map(lp("en es"), true));
        assertEquals(lp("en es-ES"), index.map(lp("en es-ES"), true));
        assertEquals(lp("en es-MX"), index.map(lp("en es-MX"), true));
        assertEquals(lp("en es-MX"), index.map(lp("en es-CO"), true));
        assertEquals(lp("en es-MX"), index.map(lp("en-US es-CO"), true));

        assertNull(index.map(lp("es en"), true));
        assertNull(index.map(lp("es-ES en"), true));
        assertNull(index.map(lp("es-MX en"), true));
        assertNull(index.map(lp("es-CO en"), true));
        assertNull(index.map(lp("es-CO en-US"), true));

        assertEquals(lp("en es-ES"), index.map(lp("en es"), false));
        assertEquals(lp("en es-ES"), index.map(lp("en es-ES"), false));
        assertEquals(lp("en es-MX"), index.map(lp("en es-MX"), false));
        assertEquals(lp("en es-MX"), index.map(lp("en es-CO"), false));
        assertEquals(lp("en es-MX"), index.map(lp("en-US es-CO"), false));

        assertNull(index.map(lp("es en"), false));
        assertNull(index.map(lp("es-ES en"), false));
        assertNull(index.map(lp("es-MX en"), false));
        assertNull(index.map(lp("es-CO en"), false));
        assertNull(index.map(lp("es-CO en-US"), false));
    }

    @Test
    public void ruledLanguageBidirectional() {
        assertEquals(lp("en zh-CN"), index.map(lp("en zh"), true));
        assertEquals(lp("en zh-CN"), index.map(lp("en zh-CN"), true));
        assertEquals(lp("en zh-CN"), index.map(lp("en zh-XX"), true));
        assertEquals(lp("en zh-TW"), index.map(lp("en zh-TW"), true));
        assertEquals(lp("en zh-TW"), index.map(lp("en zh-HK"), true));
        assertEquals(lp("en zh-TW"), index.map(lp("en-US zh-HK"), true));

        assertEquals(lp("zh en"), index.map(lp("zh en"), true));
        assertEquals(lp("zh en"), index.map(lp("zh-CN en"), true));
        assertEquals(lp("zh en"), index.map(lp("zh-XX en"), true));
        assertEquals(lp("zh en"), index.map(lp("zh-TW en"), true));
        assertEquals(lp("zh en"), index.map(lp("zh-HK en"), true));
        assertEquals(lp("zh en"), index.map(lp("zh-HK en-US"), true));

        assertEquals(lp("en zh-CN"), index.map(lp("en zh"), false));
        assertEquals(lp("en zh-CN"), index.map(lp("en zh-CN"), false));
        assertEquals(lp("en zh-CN"), index.map(lp("en zh-XX"), false));
        assertEquals(lp("en zh-TW"), index.map(lp("en zh-TW"), false));
        assertEquals(lp("en zh-TW"), index.map(lp("en zh-HK"), false));
        assertEquals(lp("en zh-TW"), index.map(lp("en-US zh-HK"), false));

        assertEquals(lp("zh-CN en"), index.map(lp("zh en"), false));
        assertEquals(lp("zh-CN en"), index.map(lp("zh-CN en"), false));
        assertEquals(lp("zh-CN en"), index.map(lp("zh-XX en"), false));
        assertEquals(lp("zh-TW en"), index.map(lp("zh-TW en"), false));
        assertEquals(lp("zh-TW en"), index.map(lp("zh-HK en"), false));
        assertEquals(lp("zh-TW en"), index.map(lp("zh-HK en-US"), false));
    }

}
