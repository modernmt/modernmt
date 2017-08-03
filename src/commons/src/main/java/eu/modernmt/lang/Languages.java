package eu.modernmt.lang;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by davide on 12/11/15.
 */
public class Languages {

    // sorted by ISO 639-1 codes
    public static final Locale ARABIC = Locale.forLanguageTag("ar");
    public static final Locale BULGARIAN = Locale.forLanguageTag("bg");
    public static final Locale BRETON = Locale.forLanguageTag("br");
    public static final Locale CATALAN = Locale.forLanguageTag("ca");
    public static final Locale CZECH = Locale.forLanguageTag("cs");
    public static final Locale DANISH = Locale.forLanguageTag("da");
    public static final Locale GERMAN = Locale.forLanguageTag("de");
    public static final Locale GREEK = Locale.forLanguageTag("el");
    public static final Locale ENGLISH = Locale.forLanguageTag("en");
    public static final Locale ESPERANTO = Locale.forLanguageTag("eo");
    public static final Locale SPANISH = Locale.forLanguageTag("es");
    public static final Locale BASQUE = Locale.forLanguageTag("eu");
    public static final Locale PERSIAN = Locale.forLanguageTag("fa");
    public static final Locale FINNISH = Locale.forLanguageTag("fi");
    public static final Locale FRENCH = Locale.forLanguageTag("fr");
    public static final Locale IRISH = Locale.forLanguageTag("ga");
    public static final Locale GALICIAN = Locale.forLanguageTag("gl");
    public static final Locale HEBREW = Locale.forLanguageTag("he");
    public static final Locale HINDI = Locale.forLanguageTag("hi");
    public static final Locale HUNGARIAN = Locale.forLanguageTag("hu");
    public static final Locale ARMENIAN = Locale.forLanguageTag("hy");
    public static final Locale INDONESIAN = Locale.forLanguageTag("id");
    public static final Locale ICELANDIC = Locale.forLanguageTag("is");
    public static final Locale ITALIAN = Locale.forLanguageTag("it");
    public static final Locale JAPANESE = Locale.forLanguageTag("ja");
    public static final Locale KHMER = Locale.forLanguageTag("km");
    public static final Locale KOREAN = Locale.forLanguageTag("ko");
    public static final Locale LATVIAN = Locale.forLanguageTag("lv");
    public static final Locale MALAYALAM = Locale.forLanguageTag("ml");
    public static final Locale DUTCH = Locale.forLanguageTag("nl");
    public static final Locale NORWEGIAN = Locale.forLanguageTag("no");
    public static final Locale POLISH = Locale.forLanguageTag("pl");
    public static final Locale BRAZILIAN = Locale.forLanguageTag("pt-BR");
    public static final Locale PORTUGUESE = Locale.forLanguageTag("pt");
    public static final Locale ROMANIAN = Locale.forLanguageTag("ro");
    public static final Locale RUSSIAN = Locale.forLanguageTag("ru");
    public static final Locale NORTHERN_SAMI = Locale.forLanguageTag("se");
    public static final Locale SLOVAK = Locale.forLanguageTag("sk");
    public static final Locale SLOVENE = Locale.forLanguageTag("sl");
    public static final Locale SWEDISH = Locale.forLanguageTag("sv");
    public static final Locale TAMIL = Locale.forLanguageTag("ta");
    public static final Locale THAI = Locale.forLanguageTag("th");
    public static final Locale TAGALOG = Locale.forLanguageTag("tl");
    public static final Locale TURKISH = Locale.forLanguageTag("tr");
    public static final Locale UKRAINIAN = Locale.forLanguageTag("uk");
    public static final Locale CHINESE = Locale.forLanguageTag("zh");


    private static final HashMap<String, Locale> LOCALES = new HashMap<>();

    static {
        try {
            for (Field field : Languages.class.getDeclaredFields()) {
                if (Locale.class.equals(field.getType())) {
                    Locale locale = (Locale) field.get(null);
                    LOCALES.put(locale.toLanguageTag(), locale);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("This cannot happen", e);
        }
    }

    public static Locale getSupportedLanguage(String languageTag) {
        return LOCALES.get(languageTag);
    }

    public static boolean sameLanguage(Locale language1, Locale language2) {
        return language1.getLanguage().equals(language2.getLanguage());
    }
}
