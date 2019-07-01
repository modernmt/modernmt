package eu.modernmt.lang;

/**
 * This class represent a Pattern matching algorithm for language tags.
 * Every pattern is made of exactly 3 tokens separated by a single whitespace;
 * their positions follow the Language semantic:
 * <p>
 * LANGUAGE SCRIPT REGION
 * <p>
 * "LANGUAGE" is always a valid language code, while the remaining two tokens can have four different values:
 * - "*": matches any value
 * - "+": matches any value different from null
 * - "NULL": matches a NULL value
 * - any other string will match with "equalsIgnoreCase"
 */
public class LanguagePattern {

    public static LanguagePattern parse(String pattern) throws IllegalArgumentException {
        String[] parts = pattern.split(" ");
        if (parts.length != 3)
            throw new IllegalArgumentException(pattern);

        String language = Language.parseLanguage(parts[0]);
        if (language == null)
            throw new IllegalArgumentException(pattern);

        return new LanguagePattern(language, Matcher.fromString(parts[1].trim()), Matcher.fromString(parts[2].trim()));
    }

    private final String language;
    private final Matcher script;
    private final Matcher region;

    private LanguagePattern(String language, Matcher script, Matcher region) {
        this.language = language;
        this.script = script;
        this.region = region;
    }

    public String getLanguage() {
        return language;
    }

    public boolean match(Language test) {
        return language.equals(test.getLanguage()) && script.match(test.getScript()) && region.match(test.getRegion());
    }

    private interface Matcher {

        static Matcher fromString(String pattern) throws IllegalArgumentException {
            if (pattern == null || pattern.length() == 0)
                throw new IllegalArgumentException(pattern);

            if ("*".equals(pattern))
                return TrueMatcher.INSTANCE;
            else if ("+".equals(pattern))
                return NotNullMatcher.INSTANCE;
            else if ("NULL".equalsIgnoreCase(pattern))
                return NullMatcher.INSTANCE;
            else
                return new ValueMatcher(pattern);
        }

        boolean match(String value);

    }

    private static final class TrueMatcher implements Matcher {

        static TrueMatcher INSTANCE = new TrueMatcher();

        @Override
        public boolean match(String value) {
            return true;
        }
    }

    private static final class NotNullMatcher implements Matcher {

        static NotNullMatcher INSTANCE = new NotNullMatcher();

        @Override
        public boolean match(String value) {
            return value != null;
        }
    }

    private static final class NullMatcher implements Matcher {

        static NullMatcher INSTANCE = new NullMatcher();

        @Override
        public boolean match(String value) {
            return value == null;
        }
    }

    private static final class ValueMatcher implements Matcher {

        private final String value;

        ValueMatcher(String value) {
            this.value = value;
        }

        @Override
        public boolean match(String value) {
            return this.value.equalsIgnoreCase(value);
        }
    }
}
