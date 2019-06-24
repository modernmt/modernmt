package eu.modernmt.lang;

import java.io.Serializable;
import java.util.regex.Pattern;

public final class Language2 implements Serializable {

    private static final Pattern IS_ALPHA = Pattern.compile("[A-Za-z]+");
    private static final Pattern IS_ALPHANUM = Pattern.compile("[A-Za-z0-9]+");
    private static final Pattern IS_DIGIT = Pattern.compile("[0-9]+");

    // sorted by ISO 639-1 codes
    public static final Language2 ARABIC = Language2.fromString("ar");
    public static final Language2 BULGARIAN = Language2.fromString("bg");
    public static final Language2 BRETON = Language2.fromString("br");
    public static final Language2 CATALAN = Language2.fromString("ca");
    public static final Language2 CZECH = Language2.fromString("cs");
    public static final Language2 DANISH = Language2.fromString("da");
    public static final Language2 GERMAN = Language2.fromString("de");
    public static final Language2 GREEK = Language2.fromString("el");
    public static final Language2 ENGLISH = Language2.fromString("en");
    public static final Language2 ESPERANTO = Language2.fromString("eo");
    public static final Language2 SPANISH = Language2.fromString("es");
    public static final Language2 BASQUE = Language2.fromString("eu");
    public static final Language2 PERSIAN = Language2.fromString("fa");
    public static final Language2 FINNISH = Language2.fromString("fi");
    public static final Language2 FRENCH = Language2.fromString("fr");
    public static final Language2 IRISH = Language2.fromString("ga");
    public static final Language2 GALICIAN = Language2.fromString("gl");
    public static final Language2 HEBREW = Language2.fromString("he");
    public static final Language2 HINDI = Language2.fromString("hi");
    public static final Language2 HUNGARIAN = Language2.fromString("hu");
    public static final Language2 ARMENIAN = Language2.fromString("hy");
    public static final Language2 INDONESIAN = Language2.fromString("id");
    public static final Language2 ICELANDIC = Language2.fromString("is");
    public static final Language2 ITALIAN = Language2.fromString("it");
    public static final Language2 JAPANESE = Language2.fromString("ja");
    public static final Language2 KHMER = Language2.fromString("km");
    public static final Language2 KOREAN = Language2.fromString("ko");
    public static final Language2 LATVIAN = Language2.fromString("lv");
    public static final Language2 MALAYALAM = Language2.fromString("ml");
    public static final Language2 DUTCH = Language2.fromString("nl");
    public static final Language2 NORWEGIAN = Language2.fromString("no");
    public static final Language2 POLISH = Language2.fromString("pl");
    public static final Language2 BRAZILIAN = Language2.fromString("pt-BR");
    public static final Language2 PORTUGUESE = Language2.fromString("pt");
    public static final Language2 ROMANIAN = Language2.fromString("ro");
    public static final Language2 RUSSIAN = Language2.fromString("ru");
    public static final Language2 NORTHERN_SAMI = Language2.fromString("se");
    public static final Language2 SLOVAK = Language2.fromString("sk");
    public static final Language2 SLOVENE = Language2.fromString("sl");
    public static final Language2 SWEDISH = Language2.fromString("sv");
    public static final Language2 TAMIL = Language2.fromString("ta");
    public static final Language2 THAI = Language2.fromString("th");
    public static final Language2 TAGALOG = Language2.fromString("tl");
    public static final Language2 TURKISH = Language2.fromString("tr");
    public static final Language2 UKRAINIAN = Language2.fromString("uk");
    public static final Language2 CHINESE = Language2.fromString("zh");
    public static final Language2 CHINESE_SIMPLIFIED = Language2.fromString("zh-CN");
    public static final Language2 CHINESE_TRADITIONAL = Language2.fromString("zh-TW");

    private static String toTitleCase(String string) {
        return Character.toUpperCase(string.charAt(0)) + string.substring(1).toLowerCase();
    }

    private static boolean isAlphanum(String string) {
        return IS_ALPHANUM.matcher(string).matches();
    }

    private static boolean isAlpha(String string) {
        return IS_ALPHA.matcher(string).matches();
    }

    private static boolean isDigit(String string) {
        return IS_DIGIT.matcher(string).matches();
    }

    private static String parseLanguage(String string) {
        if ((string.length() == 2 || string.length() == 3) && isAlpha(string))
            return string.toLowerCase();
        else
            return null;
    }

    private static String parseScript(String string) {
        if (string.length() == 4 && isAlpha(string))
            return toTitleCase(string);
        else
            return null;
    }

    private static String parseRegion(String string) {
        if (string.length() == 2 && isAlpha(string))
            return string.toUpperCase();
        else if (string.length() == 3 && isDigit(string))
            return string;
        else
            return null;
    }

    public static Language2 fromString(String string) {
        if (string == null)
            throw new NullPointerException();
        if (string.isEmpty())
            throw new IllegalArgumentException();

        StringBuilder tag = new StringBuilder(string.length());
        boolean skip = false;
        String language = null;
        String script = null;
        String region = null;

        String[] strings = string.replace('_', '-').split("-");
        for (int i = 0; i < strings.length; i++) {
            String chunk = strings[i];

            if (i == 0) {
                language = parseLanguage(chunk);
                if (language == null)
                    throw new IllegalArgumentException(string);
                tag.append(language);
            } else {
                if (!skip) {
                    if (i == 1 && script == null && (script = parseScript(chunk)) != null) {
                        tag.append('-');
                        tag.append(script);
                        continue;
                    }

                    if (i <= 2 && region == null && (region = parseRegion(chunk)) != null) {
                        tag.append('-');
                        tag.append(region);
                        continue;
                    }
                }

                tag.append('-');
                tag.append(chunk);
                skip = true;
            }
        }

        if (language == null)
            throw new IllegalArgumentException(string);

        return new Language2(language, script, region, tag.toString());
    }

    private final String language;
    private final String script;
    private final String region;
    private final String tag;

    private Language2(String language, String script, String region, String tag) {
        this.language = language;
        this.region = region;
        this.script = script;
        this.tag = tag;
    }

    /**
     * @return ISO 639-1 or 639-3 language code, eg "fr" or "gsw".
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return ISO15924 script code
     */
    public String getScript() {
        return script;
    }

    /**
     * @return ISO 3166-1 or UN M.49 code, eg "DE" or 150.
     */
    public String getRegion() {
        return region;
    }

    public String toLanguageTag(boolean script, boolean region) {
        StringBuilder result = new StringBuilder(10);
        result.append(language);

        if (script && this.script != null) {
            result.append('-');
            result.append(this.script);
        }

        if (region && this.region != null) {
            result.append('-');
            result.append(this.region);
        }

        return result.toString();
    }

    public String toLanguageTag() {
        return tag;
    }

    public String toString() {
        return tag;
    }

    public boolean isEqualOrMoreGenericThan(Language2 other) {
        if (!language.equals(other.language))
            return false;
        if (script != null && !script.equals(other.script))
            return false;
        if (region != null && !region.equals(other.region))
            return false;
        return true;
    }

    public boolean isEqualOrMoreSpecificThan(Language2 other) {
        return other.isEqualOrMoreGenericThan(this);
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

}
