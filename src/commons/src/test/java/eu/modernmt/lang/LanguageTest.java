package eu.modernmt.lang;

import org.junit.Test;

import static org.junit.Assert.*;

public class LanguageTest {
    // "es-x-mo-SDL"

    @Test
    public void language() {
        for (String string : new String[]{"en", "EN", "En"}) {
            Language language = Language.fromString(string);

            assertEquals("en", language.getLanguage());
            assertNull(language.getScript());
            assertNull(language.getRegion());
            assertEquals("en", language.toLanguageTag());
        }
    }

    @Test
    public void languageWithScript() {
        for (String string : new String[]{"en-Latn", "EN-LATN", "En-latn", "en_latn"}) {
            Language language = Language.fromString(string);

            assertEquals("en", language.getLanguage());
            assertEquals("Latn", language.getScript());
            assertNull(language.getRegion());
            assertEquals("en-Latn", language.toLanguageTag());
        }
    }

    @Test
    public void languageWithCountry() {
        for (String string : new String[]{"en-US", "EN-us", "En-Us", "en_us"}) {
            Language language = Language.fromString(string);

            assertEquals("en", language.getLanguage());
            assertNull(language.getScript());
            assertEquals("US", language.getRegion());
            assertEquals("en-US", language.toLanguageTag());
        }
    }

    @Test
    public void languageWithCountryAndScript() {
        for (String string : new String[]{"en-Latn_US", "EN-latn-us"}) {
            Language language = Language.fromString(string);

            assertEquals("en", language.getLanguage());
            assertEquals("Latn", language.getScript());
            assertEquals("US", language.getRegion());
            assertEquals("en-Latn-US", language.toLanguageTag());
        }
    }

    @Test
    public void languageWithPrivateUse() {
        for (String string : new String[]{"es-x-mo-SDL", "ES_x_mo_SDL"}) {
            Language language = Language.fromString(string);

            assertEquals("es", language.getLanguage());
            assertNull(language.getScript());
            assertNull(language.getRegion());
            assertEquals("es-x-mo-SDL", language.toLanguageTag());
        }
    }

    @Test
    public void languageWithAll() {
        for (String string : new String[]{"es-Latn-ES-japanese-x-mo-SDL", "ES_LATN_ES_japanese-x_mo_SDL"}) {
            Language language = Language.fromString(string);

            assertEquals("es", language.getLanguage());
            assertEquals("Latn", language.getScript());
            assertEquals("ES", language.getRegion());
            assertEquals("es-Latn-ES-japanese-x-mo-SDL", language.toLanguageTag());
        }
    }

    @Test
    public void testMoreGenericWithLanguage() {
        Language language = Language.fromString("es");

        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("en")));
        assertTrue(language.isEqualOrMoreGenericThan(Language.fromString("es")));
        assertTrue(language.isEqualOrMoreGenericThan(Language.fromString("es-ES")));
        assertTrue(language.isEqualOrMoreGenericThan(Language.fromString("es-Latn")));
        assertTrue(language.isEqualOrMoreGenericThan(Language.fromString("es-Latn-ES")));
    }

    @Test
    public void testMoreGenericWithLanguageAndScript() {
        Language language = Language.fromString("es-Latn");

        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("en")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es-ES")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es-Cyrl")));
        assertTrue(language.isEqualOrMoreGenericThan(Language.fromString("es-Latn")));
        assertTrue(language.isEqualOrMoreGenericThan(Language.fromString("es-Latn-ES")));
    }

    @Test
    public void testMoreGenericWithLanguageAndRegion() {
        Language language = Language.fromString("es-ES");

        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("en")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es")));
        assertTrue(language.isEqualOrMoreGenericThan(Language.fromString("es-ES")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es-MX")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es-Latn")));
        assertTrue(language.isEqualOrMoreGenericThan(Language.fromString("es-Latn-ES")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es-Latn-MX")));
    }

    @Test
    public void testMoreGenericWithLanguageScriptAndRegion() {
        Language language = Language.fromString("es-Latn-ES");

        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("en")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es-ES")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es-MX")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es-Cyrl")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es-Latn")));
        assertTrue(language.isEqualOrMoreGenericThan(Language.fromString("es-Latn-ES")));
        assertFalse(language.isEqualOrMoreGenericThan(Language.fromString("es-Latn-MX")));
    }

    @Test
    public void testToLanguageTag() {
        Language language = Language.fromString("es-Latn-ES-x-mo-SDL");

        assertEquals("es-Latn-ES-x-mo-SDL", language.toLanguageTag());
        assertEquals("es", language.toLanguageTag(false, false));
        assertEquals("es-Latn", language.toLanguageTag(true, false));
        assertEquals("es-ES", language.toLanguageTag(false, true));
        assertEquals("es-Latn-ES", language.toLanguageTag(true, true));
    }

}
