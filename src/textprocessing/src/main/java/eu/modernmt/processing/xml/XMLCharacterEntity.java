package eu.modernmt.processing.xml;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by davide on 08/03/16.
 */
public class XMLCharacterEntity {

    public static final Pattern EntityPattern = Pattern.compile("&((#[0-9]{1,4})|(#x[0-9a-fA-F]{1,4})|([a-zA-Z]+));");

    private static final HashMap<String, Character> NAMED_ENTITIES = new HashMap<>();

    static {
        NAMED_ENTITIES.put("quot", '\u0022');
        NAMED_ENTITIES.put("amp", '\u0026');
        NAMED_ENTITIES.put("apos", '\'');
        NAMED_ENTITIES.put("lt", '\u003C');
        NAMED_ENTITIES.put("gt", '\u003E');
        NAMED_ENTITIES.put("nbsp", '\u00A0');
        NAMED_ENTITIES.put("iexcl", '\u00A1');
        NAMED_ENTITIES.put("cent", '\u00A2');
        NAMED_ENTITIES.put("pound", '\u00A3');
        NAMED_ENTITIES.put("curren", '\u00A4');
        NAMED_ENTITIES.put("yen", '\u00A5');
        NAMED_ENTITIES.put("brvbar", '\u00A6');
        NAMED_ENTITIES.put("sect", '\u00A7');
        NAMED_ENTITIES.put("uml", '\u00A8');
        NAMED_ENTITIES.put("copy", '\u00A9');
        NAMED_ENTITIES.put("ordf", '\u00AA');
        NAMED_ENTITIES.put("laquo", '\u00AB');
        NAMED_ENTITIES.put("not", '\u00AC');
        NAMED_ENTITIES.put("shy", '\u00AD');
        NAMED_ENTITIES.put("reg", '\u00AE');
        NAMED_ENTITIES.put("macr", '\u00AF');
        NAMED_ENTITIES.put("deg", '\u00B0');
        NAMED_ENTITIES.put("plusmn", '\u00B1');
        NAMED_ENTITIES.put("sup2", '\u00B2');
        NAMED_ENTITIES.put("sup3", '\u00B3');
        NAMED_ENTITIES.put("acute", '\u00B4');
        NAMED_ENTITIES.put("micro", '\u00B5');
        NAMED_ENTITIES.put("para", '\u00B6');
        NAMED_ENTITIES.put("middot", '\u00B7');
        NAMED_ENTITIES.put("cedil", '\u00B8');
        NAMED_ENTITIES.put("sup1", '\u00B9');
        NAMED_ENTITIES.put("ordm", '\u00BA');
        NAMED_ENTITIES.put("raquo", '\u00BB');
        NAMED_ENTITIES.put("frac14", '\u00BC');
        NAMED_ENTITIES.put("frac12", '\u00BD');
        NAMED_ENTITIES.put("frac34", '\u00BE');
        NAMED_ENTITIES.put("iquest", '\u00BF');
        NAMED_ENTITIES.put("Agrave", '\u00C0');
        NAMED_ENTITIES.put("Aacute", '\u00C1');
        NAMED_ENTITIES.put("Acirc", '\u00C2');
        NAMED_ENTITIES.put("Atilde", '\u00C3');
        NAMED_ENTITIES.put("Auml", '\u00C4');
        NAMED_ENTITIES.put("Aring", '\u00C5');
        NAMED_ENTITIES.put("AElig", '\u00C6');
        NAMED_ENTITIES.put("Ccedil", '\u00C7');
        NAMED_ENTITIES.put("Egrave", '\u00C8');
        NAMED_ENTITIES.put("Eacute", '\u00C9');
        NAMED_ENTITIES.put("Ecirc", '\u00CA');
        NAMED_ENTITIES.put("Euml", '\u00CB');
        NAMED_ENTITIES.put("Igrave", '\u00CC');
        NAMED_ENTITIES.put("Iacute", '\u00CD');
        NAMED_ENTITIES.put("Icirc", '\u00CE');
        NAMED_ENTITIES.put("Iuml", '\u00CF');
        NAMED_ENTITIES.put("ETH", '\u00D0');
        NAMED_ENTITIES.put("Ntilde", '\u00D1');
        NAMED_ENTITIES.put("Ograve", '\u00D2');
        NAMED_ENTITIES.put("Oacute", '\u00D3');
        NAMED_ENTITIES.put("Ocirc", '\u00D4');
        NAMED_ENTITIES.put("Otilde", '\u00D5');
        NAMED_ENTITIES.put("Ouml", '\u00D6');
        NAMED_ENTITIES.put("times", '\u00D7');
        NAMED_ENTITIES.put("Oslash", '\u00D8');
        NAMED_ENTITIES.put("Ugrave", '\u00D9');
        NAMED_ENTITIES.put("Uacute", '\u00DA');
        NAMED_ENTITIES.put("Ucirc", '\u00DB');
        NAMED_ENTITIES.put("Uuml", '\u00DC');
        NAMED_ENTITIES.put("Yacute", '\u00DD');
        NAMED_ENTITIES.put("THORN", '\u00DE');
        NAMED_ENTITIES.put("szlig", '\u00DF');
        NAMED_ENTITIES.put("agrave", '\u00E0');
        NAMED_ENTITIES.put("aacute", '\u00E1');
        NAMED_ENTITIES.put("acirc", '\u00E2');
        NAMED_ENTITIES.put("atilde", '\u00E3');
        NAMED_ENTITIES.put("auml", '\u00E4');
        NAMED_ENTITIES.put("aring", '\u00E5');
        NAMED_ENTITIES.put("aelig", '\u00E6');
        NAMED_ENTITIES.put("ccedil", '\u00E7');
        NAMED_ENTITIES.put("egrave", '\u00E8');
        NAMED_ENTITIES.put("eacute", '\u00E9');
        NAMED_ENTITIES.put("ecirc", '\u00EA');
        NAMED_ENTITIES.put("euml", '\u00EB');
        NAMED_ENTITIES.put("igrave", '\u00EC');
        NAMED_ENTITIES.put("iacute", '\u00ED');
        NAMED_ENTITIES.put("icirc", '\u00EE');
        NAMED_ENTITIES.put("iuml", '\u00EF');
        NAMED_ENTITIES.put("eth", '\u00F0');
        NAMED_ENTITIES.put("ntilde", '\u00F1');
        NAMED_ENTITIES.put("ograve", '\u00F2');
        NAMED_ENTITIES.put("oacute", '\u00F3');
        NAMED_ENTITIES.put("ocirc", '\u00F4');
        NAMED_ENTITIES.put("otilde", '\u00F5');
        NAMED_ENTITIES.put("ouml", '\u00F6');
        NAMED_ENTITIES.put("divide", '\u00F7');
        NAMED_ENTITIES.put("oslash", '\u00F8');
        NAMED_ENTITIES.put("ugrave", '\u00F9');
        NAMED_ENTITIES.put("uacute", '\u00FA');
        NAMED_ENTITIES.put("ucirc", '\u00FB');
        NAMED_ENTITIES.put("uuml", '\u00FC');
        NAMED_ENTITIES.put("yacute", '\u00FD');
        NAMED_ENTITIES.put("thorn", '\u00FE');
        NAMED_ENTITIES.put("yuml", '\u00FF');
        NAMED_ENTITIES.put("OElig", '\u0152');
        NAMED_ENTITIES.put("oelig", '\u0153');
        NAMED_ENTITIES.put("Scaron", '\u0160');
        NAMED_ENTITIES.put("scaron", '\u0161');
        NAMED_ENTITIES.put("Yuml", '\u0178');
        NAMED_ENTITIES.put("fnof", '\u0192');
        NAMED_ENTITIES.put("circ", '\u02C6');
        NAMED_ENTITIES.put("tilde", '\u02DC');
        NAMED_ENTITIES.put("Alpha", '\u0391');
        NAMED_ENTITIES.put("Beta", '\u0392');
        NAMED_ENTITIES.put("Gamma", '\u0393');
        NAMED_ENTITIES.put("Delta", '\u0394');
        NAMED_ENTITIES.put("Epsilon", '\u0395');
        NAMED_ENTITIES.put("Zeta", '\u0396');
        NAMED_ENTITIES.put("Eta", '\u0397');
        NAMED_ENTITIES.put("Theta", '\u0398');
        NAMED_ENTITIES.put("Iota", '\u0399');
        NAMED_ENTITIES.put("Kappa", '\u039A');
        NAMED_ENTITIES.put("Lambda", '\u039B');
        NAMED_ENTITIES.put("Mu", '\u039C');
        NAMED_ENTITIES.put("Nu", '\u039D');
        NAMED_ENTITIES.put("Xi", '\u039E');
        NAMED_ENTITIES.put("Omicron", '\u039F');
        NAMED_ENTITIES.put("Pi", '\u03A0');
        NAMED_ENTITIES.put("Rho", '\u03A1');
        NAMED_ENTITIES.put("Sigma", '\u03A3');
        NAMED_ENTITIES.put("Tau", '\u03A4');
        NAMED_ENTITIES.put("Upsilon", '\u03A5');
        NAMED_ENTITIES.put("Phi", '\u03A6');
        NAMED_ENTITIES.put("Chi", '\u03A7');
        NAMED_ENTITIES.put("Psi", '\u03A8');
        NAMED_ENTITIES.put("Omega", '\u03A9');
        NAMED_ENTITIES.put("alpha", '\u03B1');
        NAMED_ENTITIES.put("beta", '\u03B2');
        NAMED_ENTITIES.put("gamma", '\u03B3');
        NAMED_ENTITIES.put("delta", '\u03B4');
        NAMED_ENTITIES.put("epsilon", '\u03B5');
        NAMED_ENTITIES.put("zeta", '\u03B6');
        NAMED_ENTITIES.put("eta", '\u03B7');
        NAMED_ENTITIES.put("theta", '\u03B8');
        NAMED_ENTITIES.put("iota", '\u03B9');
        NAMED_ENTITIES.put("kappa", '\u03BA');
        NAMED_ENTITIES.put("lambda", '\u03BB');
        NAMED_ENTITIES.put("mu", '\u03BC');
        NAMED_ENTITIES.put("nu", '\u03BD');
        NAMED_ENTITIES.put("xi", '\u03BE');
        NAMED_ENTITIES.put("omicron", '\u03BF');
        NAMED_ENTITIES.put("pi", '\u03C0');
        NAMED_ENTITIES.put("rho", '\u03C1');
        NAMED_ENTITIES.put("sigmaf", '\u03C2');
        NAMED_ENTITIES.put("sigma", '\u03C3');
        NAMED_ENTITIES.put("tau", '\u03C4');
        NAMED_ENTITIES.put("upsilon", '\u03C5');
        NAMED_ENTITIES.put("phi", '\u03C6');
        NAMED_ENTITIES.put("chi", '\u03C7');
        NAMED_ENTITIES.put("psi", '\u03C8');
        NAMED_ENTITIES.put("omega", '\u03C9');
        NAMED_ENTITIES.put("thetasym", '\u03D1');
        NAMED_ENTITIES.put("upsih", '\u03D2');
        NAMED_ENTITIES.put("piv", '\u03D6');
        NAMED_ENTITIES.put("ensp", '\u2002');
        NAMED_ENTITIES.put("emsp", '\u2003');
        NAMED_ENTITIES.put("thinsp", '\u2009');
        NAMED_ENTITIES.put("zwnj", '\u200C');
        NAMED_ENTITIES.put("zwj", '\u200D');
        NAMED_ENTITIES.put("lrm", '\u200E');
        NAMED_ENTITIES.put("rlm", '\u200F');
        NAMED_ENTITIES.put("ndash", '\u2013');
        NAMED_ENTITIES.put("mdash", '\u2014');
        NAMED_ENTITIES.put("lsquo", '\u2018');
        NAMED_ENTITIES.put("rsquo", '\u2019');
        NAMED_ENTITIES.put("sbquo", '\u201A');
        NAMED_ENTITIES.put("ldquo", '\u201C');
        NAMED_ENTITIES.put("rdquo", '\u201D');
        NAMED_ENTITIES.put("bdquo", '\u201E');
        NAMED_ENTITIES.put("dagger", '\u2020');
        NAMED_ENTITIES.put("Dagger", '\u2021');
        NAMED_ENTITIES.put("bull", '\u2022');
        NAMED_ENTITIES.put("hellip", '\u2026');
        NAMED_ENTITIES.put("permil", '\u2030');
        NAMED_ENTITIES.put("prime", '\u2032');
        NAMED_ENTITIES.put("Prime", '\u2033');
        NAMED_ENTITIES.put("lsaquo", '\u2039');
        NAMED_ENTITIES.put("rsaquo", '\u203A');
        NAMED_ENTITIES.put("oline", '\u203E');
        NAMED_ENTITIES.put("frasl", '\u2044');
        NAMED_ENTITIES.put("euro", '\u20AC');
        NAMED_ENTITIES.put("image", '\u2111');
        NAMED_ENTITIES.put("weierp", '\u2118');
        NAMED_ENTITIES.put("real", '\u211C');
        NAMED_ENTITIES.put("trade", '\u2122');
        NAMED_ENTITIES.put("alefsym", '\u2135');
        NAMED_ENTITIES.put("larr", '\u2190');
        NAMED_ENTITIES.put("uarr", '\u2191');
        NAMED_ENTITIES.put("rarr", '\u2192');
        NAMED_ENTITIES.put("darr", '\u2193');
        NAMED_ENTITIES.put("harr", '\u2194');
        NAMED_ENTITIES.put("crarr", '\u21B5');
        NAMED_ENTITIES.put("lArr", '\u21D0');
        NAMED_ENTITIES.put("uArr", '\u21D1');
        NAMED_ENTITIES.put("rArr", '\u21D2');
        NAMED_ENTITIES.put("dArr", '\u21D3');
        NAMED_ENTITIES.put("hArr", '\u21D4');
        NAMED_ENTITIES.put("forall", '\u2200');
        NAMED_ENTITIES.put("part", '\u2202');
        NAMED_ENTITIES.put("exist", '\u2203');
        NAMED_ENTITIES.put("empty", '\u2205');
        NAMED_ENTITIES.put("nabla", '\u2207');
        NAMED_ENTITIES.put("isin", '\u2208');
        NAMED_ENTITIES.put("notin", '\u2209');
        NAMED_ENTITIES.put("ni", '\u220B');
        NAMED_ENTITIES.put("prod", '\u220F');
        NAMED_ENTITIES.put("sum", '\u2211');
        NAMED_ENTITIES.put("minus", '\u2212');
        NAMED_ENTITIES.put("lowast", '\u2217');
        NAMED_ENTITIES.put("radic", '\u221A');
        NAMED_ENTITIES.put("prop", '\u221D');
        NAMED_ENTITIES.put("infin", '\u221E');
        NAMED_ENTITIES.put("ang", '\u2220');
        NAMED_ENTITIES.put("and", '\u2227');
        NAMED_ENTITIES.put("or", '\u2228');
        NAMED_ENTITIES.put("cap", '\u2229');
        NAMED_ENTITIES.put("cup", '\u222A');
        NAMED_ENTITIES.put("int", '\u222B');
        NAMED_ENTITIES.put("there4", '\u2234');
        NAMED_ENTITIES.put("sim", '\u223C');
        NAMED_ENTITIES.put("cong", '\u2245');
        NAMED_ENTITIES.put("asymp", '\u2248');
        NAMED_ENTITIES.put("ne", '\u2260');
        NAMED_ENTITIES.put("equiv", '\u2261');
        NAMED_ENTITIES.put("le", '\u2264');
        NAMED_ENTITIES.put("ge", '\u2265');
        NAMED_ENTITIES.put("sub", '\u2282');
        NAMED_ENTITIES.put("sup", '\u2283');
        NAMED_ENTITIES.put("nsub", '\u2284');
        NAMED_ENTITIES.put("sube", '\u2286');
        NAMED_ENTITIES.put("supe", '\u2287');
        NAMED_ENTITIES.put("oplus", '\u2295');
        NAMED_ENTITIES.put("otimes", '\u2297');
        NAMED_ENTITIES.put("perp", '\u22A5');
        NAMED_ENTITIES.put("sdot", '\u22C5');
        NAMED_ENTITIES.put("lceil", '\u2308');
        NAMED_ENTITIES.put("rceil", '\u2309');
        NAMED_ENTITIES.put("lfloor", '\u230A');
        NAMED_ENTITIES.put("rfloor", '\u230B');
        NAMED_ENTITIES.put("lang", '\u2329');
        NAMED_ENTITIES.put("rang", '\u232A');
        NAMED_ENTITIES.put("loz", '\u25CA');
        NAMED_ENTITIES.put("spades", '\u2660');
        NAMED_ENTITIES.put("clubs", '\u2663');
        NAMED_ENTITIES.put("hearts", '\u2665');
        NAMED_ENTITIES.put("diams", '\u2666');
    }

    public static Character unescape(String entity) {
        if (entity.charAt(1) == '#') {
            if (entity.charAt(2) == 'x') {
                String content = entity.substring(3, entity.length() - 1);
                return (char) Integer.parseInt(content, 16);
            } else {
                String content = entity.substring(2, entity.length() - 1);
                return (char) Integer.parseInt(content);
            }
        } else {
            String content = entity.substring(1, entity.length() - 1);
            return NAMED_ENTITIES.get(content);
        }
    }

    public static String unescapeAll(String line) {
        char[] chars = null;
        StringBuilder builder = null;

        Matcher m = EntityPattern.matcher(line);
        int stringIndex = 0;

        while (m.find()) {
            if (chars == null) {
                chars = line.toCharArray();
                builder = new StringBuilder();
            }

            int mstart = m.start();
            int mend = m.end();

            if (stringIndex < mstart)
                builder.append(chars, stringIndex, mstart - stringIndex);

            String entity = m.group();
            Character c = XMLCharacterEntity.unescape(entity);
            if (c == null) {
                builder.append(entity);
            } else {
                builder.append(c);
            }

            stringIndex = mend;
        }

        if (builder == null)
            return line;

        if (stringIndex < chars.length)
            builder.append(chars, stringIndex, chars.length - stringIndex);

        return builder.toString();
    }
}
