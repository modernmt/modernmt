package eu.modernmt.lang;

class LanguageRule {

    private final Language2 pattern;
    private final Language2 outputLanguage;

    public LanguageRule(Language2 pattern, Language2 outputLanguage) {
        this.pattern = pattern;
        this.outputLanguage = outputLanguage;
    }

    public String getLanguage() {
        return pattern.getLanguage();
    }

    public boolean match(Language2 language) {
        return pattern.isEqualOrMoreGenericThan(language);
    }

    public Language2 getOutputLanguage() {
        return outputLanguage;
    }

}
