package eu.modernmt.lang;

import java.io.Serializable;

public final class Language implements Serializable, Comparable<Language> {

    // sorted by ISO 639-1 codes
    public static final Language ARABIC = Language.fromString("ar");
    public static final Language BULGARIAN = Language.fromString("bg");
    public static final Language BRETON = Language.fromString("br");
    public static final Language CATALAN = Language.fromString("ca");
    public static final Language CZECH = Language.fromString("cs");
    public static final Language DANISH = Language.fromString("da");
    public static final Language GERMAN = Language.fromString("de");
    public static final Language GREEK = Language.fromString("el");
    public static final Language ENGLISH = Language.fromString("en");
    public static final Language ESPERANTO = Language.fromString("eo");
    public static final Language SPANISH = Language.fromString("es");
    public static final Language BASQUE = Language.fromString("eu");
    public static final Language PERSIAN = Language.fromString("fa");
    public static final Language FINNISH = Language.fromString("fi");
    public static final Language FRENCH = Language.fromString("fr");
    public static final Language IRISH = Language.fromString("ga");
    public static final Language GALICIAN = Language.fromString("gl");
    public static final Language HEBREW = Language.fromString("he");
    public static final Language HINDI = Language.fromString("hi");
    public static final Language HUNGARIAN = Language.fromString("hu");
    public static final Language ARMENIAN = Language.fromString("hy");
    public static final Language INDONESIAN = Language.fromString("id");
    public static final Language ICELANDIC = Language.fromString("is");
    public static final Language ITALIAN = Language.fromString("it");
    public static final Language JAPANESE = Language.fromString("ja");
    public static final Language KHMER = Language.fromString("km");
    public static final Language KOREAN = Language.fromString("ko");
    public static final Language LATVIAN = Language.fromString("lv");
    public static final Language MALAYALAM = Language.fromString("ml");
    public static final Language DUTCH = Language.fromString("nl");
    public static final Language NORWEGIAN = Language.fromString("no");
    public static final Language POLISH = Language.fromString("pl");
    public static final Language BRAZILIAN = Language.fromString("pt-BR");
    public static final Language PORTUGUESE = Language.fromString("pt");
    public static final Language ROMANIAN = Language.fromString("ro");
    public static final Language RUSSIAN = Language.fromString("ru");
    public static final Language NORTHERN_SAMI = Language.fromString("se");
    public static final Language SLOVAK = Language.fromString("sk");
    public static final Language SLOVENE = Language.fromString("sl");
    public static final Language SWEDISH = Language.fromString("sv");
    public static final Language TAMIL = Language.fromString("ta");
    public static final Language THAI = Language.fromString("th");
    public static final Language TAGALOG = Language.fromString("tl");
    public static final Language TURKISH = Language.fromString("tr");
    public static final Language UKRAINIAN = Language.fromString("uk");
    public static final Language CHINESE = Language.fromString("zh");
    public static final Language CHINESE_SIMPLIFIED = Language.fromString("zh-CN");
    public static final Language CHINESE_TRADITIONAL = Language.fromString("zh-TW");

    private final String language;
    private final String region;
    private final String tag;

    public Language(String language) {
        this(language, null);
    }

    public Language(String language, String region) {
        this.language = language;
        this.region = region;

        StringBuilder sb = new StringBuilder();
        sb.append(language);

        if (region != null) {
            sb.append('-');
            sb.append(region);
        }

        this.tag = sb.toString();
    }

    public static Language fromString(String string) {
        if (string == null)
            throw new NullPointerException();
        if (string.isEmpty())
            throw new IllegalArgumentException();

        String language = null;
        String script = null;
        String region = null;

        String[] strings = string.split("-");
        for (int i = 0; i < strings.length; i++) {
            String chunk = strings[i];

            if (i == 0) {
                if (!looksLikeLanguage(chunk))
                    throw new IllegalArgumentException(string);

                language = chunk;
            } else {
                if (script == null && region == null && looksLikeScriptCode(chunk))
                    script = chunk;
                else if (region == null && (looksLikeGeoCode3166_1(chunk) || looksLikeGeoCodeNumeric(chunk)))
                    region = chunk;
                else
                    throw new IllegalArgumentException(string);
            }
        }

        assert language != null;

        return new Language(language, region);
    }

    private static boolean looksLikeScriptCode(String string) {
        return string.length() == 4 && string.matches("[A-Z][a-z]{3}");
    }

    private static boolean looksLikeGeoCode3166_1(String string) {
        return string.length() == 2 && string.matches("[A-Z]{2}");
    }

    private static boolean looksLikeGeoCodeNumeric(String string) {
        return string.length() == 3 && string.matches("[0-9]{3}");
    }

    private static boolean looksLikeLanguage(String string) {
        return string.matches("[a-z]{2,3}");
    }

    public String toLanguageTag() {
        return tag;
    }

    public String toString() {
        return tag;
    }


    /**
     * @return ISO 639-1 or 639-3 language code, eg "fr" or "gsw".
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return ISO 3166-1 or UN M.49 code, eg "DE" or 150.
     */
    public String getRegion() {
        return region;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Language language1 = (Language) o;

        if (!language.equals(language1.language)) return false;
        return region != null ? region.equals(language1.region) : language1.region == null;
    }

    @Override
    public int hashCode() {
        int result = language.hashCode();
        result = 31 * result + (region != null ? region.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Language o) {
        int cmp = language.compareTo(o.language);
        if (cmp != 0)
            return cmp;

        if (region == null)
            return o.region == null ? 0 : -1;

        return o.region == null ? 1 : region.compareTo(o.region);
    }
}
