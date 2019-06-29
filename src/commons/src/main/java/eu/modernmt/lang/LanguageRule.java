package eu.modernmt.lang;

class LanguageRule {

    public static LanguageRule make(String pattern, Language2 output) throws IllegalArgumentException {
        return new LanguageRule(LanguageMatcher.parse(pattern), output);
    }

    private final LanguageMatcher matcher;
    private final Language2 outputLanguage;

    private LanguageRule(LanguageMatcher matcher, Language2 outputLanguage) {
        this.matcher = matcher;
        this.outputLanguage = outputLanguage;
    }

    public String getLanguage() {
        return matcher.getLanguage();
    }

    public boolean match(Language2 language) {
        return matcher.match(language);
    }

    public Language2 getOutputLanguage() {
        return outputLanguage;
    }

}
