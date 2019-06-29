package eu.modernmt.lang;

class LanguageMatcher {

    private static final String MATCH_ALL = "*";

    private final String language;
    private final String script;
    private final String region;

    /**
     * Parse a pattern represented by a string with following syntax:
     *   - A pattern has a structure similar to IETF BCP 47 language tag, with the exception of the match-all symbol '*'
     *   - A pattern has a minimum of 1 parameter and a maximum of 3 parameters (i.e. 'es' and 'es-Latn-419')
     *   - The match-all symbol '*' can be used only for region and script attributes, NOT language
     *   - A match-all symbol '*' means that the Matcher should just ignore the field where the symbol is present
     *   - A missing part (region and/or script) means that the Matcher should accept only MISSING values in the matching language
     *   - A regular value means that the matching language must expose the same exact value of the matcher part
     *   - The special value '(lang)-*' is expanded into '(lang)-*-*' otherwise it would be impossible to distinct between
     *     a match-all symbol on script or region
     *
     * @param pattern the pattern
     * @return the LanguageMatcher corresponding to given pattern
     * @throws IllegalArgumentException if the pattern breaks the described syntax
     */
    public static LanguageMatcher parse(String pattern) throws IllegalArgumentException {
        if (pattern == null)
            throw new NullPointerException();
        pattern = pattern.trim();
        if (pattern.isEmpty())
            throw new IllegalArgumentException();

        String script = null;
        String region = null;

        String[] strings = pattern.split("-");
        if (strings.length > 3)
            throw new IllegalArgumentException(pattern);

        String language = Language2.parseLanguage(strings[0]);
        if (language == null)
            throw new IllegalArgumentException(pattern);


        if (strings.length == 2) {
            if (MATCH_ALL.equals(strings[1])) {
                // The special value '(lang)-*' is expanded into '(lang)-*-*'
                // otherwise it would be impossible to distinct between a match-all symbol on script or region
                script = MATCH_ALL;
                region = MATCH_ALL;
            } else {
                script = Language2.parseScript(strings[1]);
                if (script == null) {
                    region = Language2.parseRegion(strings[1]);
                    if (region == null)
                        throw new IllegalArgumentException(pattern);
                } else {
                    region = null;
                }
            }
        } else if (strings.length == 3) {
            script = MATCH_ALL.equals(strings[1]) ? MATCH_ALL : Language2.parseScript(strings[1]);
            if (script == null)
                throw new IllegalArgumentException(pattern);

            region = MATCH_ALL.equals(strings[2]) ? MATCH_ALL : Language2.parseRegion(strings[2]);
            if (region == null)
                throw new IllegalArgumentException(pattern);
        }

        return new LanguageMatcher(language, script, region);
    }

    public LanguageMatcher(String language, String script, String region) {
        this.language = language;
        this.script = script;
        this.region = region;
    }

    public boolean match(Language2 language) {
        if (!this.language.equals(language.getLanguage()))
            return false;
        if (!match(this.script, language.getScript()))
            return false;
        return match(this.region, language.getRegion());
    }

    private static boolean match(String pattern, String value) {
        if (pattern == null)
            return value == null;
        else if (MATCH_ALL.equals(pattern))
            return true;
        else
            return pattern.equals(value);
    }

    public String getLanguage() {
        return language;
    }
}
