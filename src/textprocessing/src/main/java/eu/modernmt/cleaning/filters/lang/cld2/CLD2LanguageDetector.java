package eu.modernmt.cleaning.filters.lang.cld2;

import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CLD2LanguageDetector {

    private static final Language[] LANGUAGES = new Language[]{
            new Language("en"), new Language("da"), new Language("nl"), new Language("fi"), new Language("fr"),
            new Language("de"), new Language("he"), new Language("it"), new Language("ja"), new Language("ko"),
            new Language("no"), new Language("pl"), new Language("pt"), new Language("ru"), new Language("es"),
            new Language("sv"), new Language("zh"), new Language("cs"), new Language("el"), new Language("is"),
            new Language("lv"), new Language("lt"), new Language("ro"), new Language("hu"), new Language("et"),
            null, null, new Language("bg"), new Language("hr"), new Language("sr"), new Language("ga"),
            new Language("gl"), new Language("tl"), new Language("tr"), new Language("uk"), new Language("hi"),
            new Language("mk"), new Language("bn"), new Language("id"), new Language("la"), new Language("ms"),
            new Language("ml"), new Language("cy"), new Language("ne"), new Language("te"), new Language("sq"),
            new Language("ta"), new Language("be"), new Language("jw"), new Language("oc"), new Language("ur"),
            new Language("bh"), new Language("gu"), new Language("th"), new Language("ar"), new Language("ca"),
            new Language("eo"), new Language("eu"), new Language("ia"), new Language("kn"), new Language("pa"),
            new Language("gd"), new Language("sw"), new Language("sl"), new Language("mr"), new Language("mt"),
            new Language("vi"), new Language("fy"), new Language("sk"), new Language("zh", "TW"),
            new Language("fo"), new Language("su"), new Language("uz"), new Language("am"), new Language("az"),
            new Language("ka"), new Language("ti"), new Language("fa"), new Language("bs"), new Language("si"),
            new Language("nn"), null, null, new Language("xh"), new Language("zu"), new Language("gn"),
            new Language("st"), new Language("tk"), new Language("ky"), new Language("br"), new Language("tw"),
            new Language("yi"), null, new Language("so"), new Language("ug"), new Language("ku"), new Language("mn"),
            new Language("hy"), new Language("lo"), new Language("sd"), new Language("rm"), new Language("af"),
            new Language("lb"), new Language("my"), new Language("km"), new Language("bo"), new Language("dv"),
            new Language("chr"), new Language("syr"), new Language("lif"), new Language("or"), new Language("as"),
            new Language("co"), new Language("ie"), new Language("kk"), new Language("ln"), null, new Language("ps"),
            new Language("qu"), new Language("sn"), new Language("tg"), new Language("tt"), new Language("to"),
            new Language("yo"), null, null, null, null, new Language("mi"), new Language("wo"), new Language("ab"),
            new Language("aa"), new Language("ay"), new Language("ba"), new Language("bi"), new Language("dz"),
            new Language("fj"), new Language("kl"), new Language("ha"), new Language("ht"), new Language("ik"),
            new Language("iu"), new Language("ks"), new Language("rw"), new Language("mg"), new Language("na"),
            new Language("om"), new Language("rn"), new Language("sm"), new Language("sg"), new Language("sa"),
            new Language("ss"), new Language("ts"), new Language("tn"), new Language("vo"), new Language("za"),
            new Language("kha"), new Language("sco"), new Language("lg"), new Language("gv"),
            new Language("sr", "ME"), new Language("ak"), new Language("ig"), new Language("mfe"),
            new Language("haw"), new Language("ceb"), new Language("ee"), new Language("gaa"), new Language("blu"),
            new Language("kri"), new Language("loz"), new Language("lua"), new Language("luo"), new Language("new"),
            new Language("ny"), new Language("os"), new Language("pam"), new Language("nso"), new Language("raj"),
            new Language("crs"), new Language("tum"), new Language("ve"), new Language("war")};

    private static final Logger logger = LogManager.getLogger(CLD2LanguageFilter.class);

    static {
        try {
            System.loadLibrary("jcld2");
        } catch (Throwable e) {
            logger.error("Unable to load library 'jcld2'", e);
            throw e;
        }
    }

    public Language detect(String text, boolean reliableOnly) {
        int lang = detectLanguage(text.getBytes(UTF8Charset.get()), reliableOnly);
        return 0 < lang && lang < LANGUAGES.length ? LANGUAGES[lang] : null;
    }

    private native int detectLanguage(byte[] utf8text, boolean reliableOnly);

}
