package eu.modernmt.lang;

import org.junit.Test;

import static org.junit.Assert.*;

public class LanguageTest {
    // "es-x-mo-SDL"

    @Test
    public void language() {
        for (String string : new String[]{"en", "EN", "En"}) {
            Language2 language = Language2.fromString(string);

            assertEquals("en", language.getLanguage());
            assertNull(language.getScript());
            assertNull(language.getRegion());
            assertEquals("en", language.toLanguageTag());
        }
    }

    @Test
    public void languageWithScript() {
        for (String string : new String[]{"en-Latn", "EN-LATN", "En-latn", "en_latn"}) {
            Language2 language = Language2.fromString(string);

            assertEquals("en", language.getLanguage());
            assertEquals("Latn", language.getScript());
            assertNull(language.getRegion());
            assertEquals("en-Latn", language.toLanguageTag());
        }
    }

    @Test
    public void languageWithCountry() {
        for (String string : new String[]{"en-US", "EN-us", "En-Us", "en_us"}) {
            Language2 language = Language2.fromString(string);

            assertEquals("en", language.getLanguage());
            assertNull(language.getScript());
            assertEquals("US", language.getRegion());
            assertEquals("en-US", language.toLanguageTag());
        }
    }

    @Test
    public void languageWithCountryAndScript() {
        for (String string : new String[]{"en-Latn_US", "EN-latn-us"}) {
            Language2 language = Language2.fromString(string);

            assertEquals("en", language.getLanguage());
            assertEquals("Latn", language.getScript());
            assertEquals("US", language.getRegion());
            assertEquals("en-Latn-US", language.toLanguageTag());
        }
    }

    @Test
    public void languageWithPrivateUse() {
        for (String string : new String[]{"es-x-mo-SDL", "ES_x_mo_SDL"}) {
            Language2 language = Language2.fromString(string);

            assertEquals("es", language.getLanguage());
            assertNull(language.getScript());
            assertNull(language.getRegion());
            assertEquals("es-x-mo-SDL", language.toLanguageTag());
        }
    }

    @Test
    public void languageWithAll() {
        for (String string : new String[]{"es-Latn-ES-japanese-x-mo-SDL", "ES_LATN_ES_japanese-x_mo_SDL"}) {
            Language2 language = Language2.fromString(string);

            assertEquals("es", language.getLanguage());
            assertEquals("Latn", language.getScript());
            assertEquals("ES", language.getRegion());
            assertEquals("es-Latn-ES-japanese-x-mo-SDL", language.toLanguageTag());
        }
    }

    @Test
    public void testMoreGenericWithLanguage() {
        Language2 language = Language2.fromString("es");

        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("en")));
        assertTrue(language.isEqualOrMoreGenericThan(Language2.fromString("es")));
        assertTrue(language.isEqualOrMoreGenericThan(Language2.fromString("es-ES")));
        assertTrue(language.isEqualOrMoreGenericThan(Language2.fromString("es-Latn")));
        assertTrue(language.isEqualOrMoreGenericThan(Language2.fromString("es-Latn-ES")));
    }

    @Test
    public void testMoreGenericWithLanguageAndScript() {
        Language2 language = Language2.fromString("es-Latn");

        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("en")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es-ES")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es-Cyrl")));
        assertTrue(language.isEqualOrMoreGenericThan(Language2.fromString("es-Latn")));
        assertTrue(language.isEqualOrMoreGenericThan(Language2.fromString("es-Latn-ES")));
    }

    @Test
    public void testMoreGenericWithLanguageAndRegion() {
        Language2 language = Language2.fromString("es-ES");

        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("en")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es")));
        assertTrue(language.isEqualOrMoreGenericThan(Language2.fromString("es-ES")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es-MX")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es-Latn")));
        assertTrue(language.isEqualOrMoreGenericThan(Language2.fromString("es-Latn-ES")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es-Latn-MX")));
    }

    @Test
    public void testMoreGenericWithLanguageScriptAndRegion() {
        Language2 language = Language2.fromString("es-Latn-ES");

        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("en")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es-ES")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es-MX")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es-Cyrl")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es-Latn")));
        assertTrue(language.isEqualOrMoreGenericThan(Language2.fromString("es-Latn-ES")));
        assertFalse(language.isEqualOrMoreGenericThan(Language2.fromString("es-Latn-MX")));
    }

    @Test
    public void testToLanguageTag() {
        Language2 language = Language2.fromString("es-Latn-ES-x-mo-SDL");

        assertEquals("es-Latn-ES-x-mo-SDL", language.toLanguageTag());
        assertEquals("es", language.toLanguageTag(false, false));
        assertEquals("es-Latn", language.toLanguageTag(true, false));
        assertEquals("es-ES", language.toLanguageTag(false, true));
        assertEquals("es-Latn-ES", language.toLanguageTag(true, true));
    }

}
