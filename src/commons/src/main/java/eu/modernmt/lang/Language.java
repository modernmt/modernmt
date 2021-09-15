package eu.modernmt.lang;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A IETF BCP 47 compliant language tag
 */
public final class Language implements Serializable {

    private static final Pattern IS_ALPHA = Pattern.compile("[A-Za-z]+");
    private static final Pattern IS_ALPHANUM = Pattern.compile("[A-Za-z0-9]+");
    private static final Pattern IS_DIGIT = Pattern.compile("[0-9]+");

    public static final String LATIN_SCRIPT = "Latn";
    public static final String CYRILLIC_SCRIPT = "Cyrl";

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
    public static final Language SERBIAN = Language.fromString("sr");
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

    static String parseLanguage(String string) {
        if ((string.length() == 2 || string.length() == 3) && isAlpha(string))
            return string.toLowerCase();
        else
            return null;
    }

    static String parseScript(String string) {
        if (string.length() == 4 && isAlpha(string))
            return toTitleCase(string);
        else
            return null;
    }

    static String parseRegion(String string) {
        if (string.length() == 2 && isAlpha(string))
            return string.toUpperCase();
        else if (string.length() == 3 && isDigit(string))
            return string;
        else
            return null;
    }

    public static Language fromString(String string) {
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

        return new Language(language, script, region, tag.toString());
    }

    private final String language;
    private final String script;
    private final String region;
    private final String tag;

    public Language(String language) {
        this(language, null, null, language);
    }

    private Language(String language, String script, String region, String tag) {
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

    public boolean isEqualOrMoreGenericThan(Language other) {
        if (!language.equals(other.language))
            return false;
        if (script != null && !script.equals(other.script))
            return false;
        if (region != null && !region.equals(other.region))
            return false;
        return true;
    }

    public boolean isLanguageOnly() {
        return language.equals(tag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Language language = (Language) o;
        return tag.equals(language.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag);
    }

}
