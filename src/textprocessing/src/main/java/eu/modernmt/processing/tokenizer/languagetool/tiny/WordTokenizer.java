package eu.modernmt.processing.tokenizer.languagetool.tiny;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizes a sentence into words. Punctuation and whitespace gets their own tokens.
 * The tokenizer is a quite simple character-based one, though it knows
 * about urls and will put them in one token, if fully specified including
 * a protocol (like {@code http://foobar.org}).
 *
 * @author Daniel Naber
 */
public class WordTokenizer implements LanguageToolTokenizer {

    private static final List<String> PROTOCOLS = Collections.unmodifiableList(Arrays.asList("http", "https", "ftp"));
    private static final Pattern URL_CHARS = Pattern.compile("[a-zA-Z0-9/%$-_.+!*'(),\\?]+");

    private static final String TOKENIZING_CHARACTERS = "\u0020\u00A0\u115f" +
            "\u1160\u1680"
            + "\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007"
            + "\u2008\u2009\u200A\u200B\u200c\u200d\u200e\u200f"
            + "\u2028\u2029\u202a\u202b\u202c\u202d\u202e\u202f"
            + "\u205F\u2060\u2061\u2062\u2063\u206A\u206b\u206c\u206d"
            + "\u206E\u206F\u3000\u3164\ufeff\uffa0\ufff9\ufffa\ufffb"
            + ",.;()[]{}=*#∗×·+÷<>!?:/|\\\"'«»„”“`´‘’‛′…¿¡→‼⁇⁈⁉"
            + "—"  // em dash
            + "\t\n\r";

    /**
     * Get the protocols that the tokenizer knows about.
     *
     * @return currently {@code http}, {@code https}, and {@code ftp}
     * @since 2.1
     */
    public static List<String> getProtocols() {
        return PROTOCOLS;
    }

    /**
     * @since 3.0
     */
    public static boolean isUrl(String token) {
        for (String protocol : WordTokenizer.getProtocols()) {
            if (token.startsWith(protocol + "://") || token.startsWith("www.")) {
                return true;
            }
        }
        return false;
    }

    public WordTokenizer() {
    }

    @Override
    public List<String> tokenize(final String text) {
        final List<String> l = new ArrayList<>();
        final StringTokenizer st = new StringTokenizer(text, getTokenizingCharacters(), true);
        while (st.hasMoreElements()) {
            l.add(st.nextToken());
        }
        return joinUrls(l);
    }

    /**
     * @return The string containing the characters used by the
     * tokenizer to tokenize words.
     * @since 2.5
     */
    public String getTokenizingCharacters() {
        return TOKENIZING_CHARACTERS;
    }

    // see rfc1738 and http://stackoverflow.com/questions/1856785/characters-allowed-in-a-url
    protected List<String> joinUrls(List<String> l) {
        final List<String> newList = new ArrayList<>();
        boolean inUrl = false;
        final StringBuilder url = new StringBuilder();
        String urlQuote = null;
        for (int i = 0; i < l.size(); i++) {
            if (urlStartsAt(i, l)) {
                inUrl = true;
                if (i - 1 >= 0) {
                    urlQuote = l.get(i - 1);
                }
                url.append(l.get(i));
            } else if (inUrl && urlEndsAt(i, l, urlQuote)) {
                inUrl = false;
                urlQuote = null;
                newList.add(url.toString());
                url.setLength(0);
                newList.add(l.get(i));
            } else if (inUrl) {
                url.append(l.get(i));
            } else {
                newList.add(l.get(i));
            }
        }
        if (url.length() > 0) {
            newList.add(url.toString());
        }
        return newList;
    }

    private boolean urlStartsAt(int i, List<String> l) {
        final String token = l.get(i);
        if (isProtocol(token) && l.size() > i + 3) {
            final String nToken = l.get(i + 1);
            final String nnToken = l.get(i + 2);
            final String nnnToken = l.get(i + 3);
            if (nToken.equals(":") && nnToken.equals("/") && nnnToken.equals("/")) {
                return true;
            }
        }
        if (l.size() > i + 1) {
            final String nToken = l.get(i);
            final String nnToken = l.get(i + 1);
            if (nToken.equals("www") && nnToken.equals(".")) {
                return true;
            }
        }
        return false;
    }

    private boolean isProtocol(String token) {
        for (String protocol : PROTOCOLS) {
            if (token.equals(protocol)) {
                return true;
            }
        }
        return false;
    }

    private boolean urlEndsAt(int i, List<String> l, String urlQuote) {
        final String token = l.get(i);
        if (StringTools.isWhitespace(token)) {
            return true;
        } else if (token.equals(")") || token.equals("]")) {   // this is guesswork
            return true;
        } else if (l.size() > i + 1) {
            final String nToken = l.get(i + 1);
            if (StringTools.isWhitespace(nToken) &&
                    (token.equals(".") || token.equals(",") || token.equals(";") || token.equals(":") || token.equals("!") || token.equals("?") || token.equals(urlQuote))) {
                return true;
            }
        } else {
            final Matcher matcher = URL_CHARS.matcher(token);
            if (!matcher.matches()) {
                return true;
            }
        }
        return false;
    }

}
