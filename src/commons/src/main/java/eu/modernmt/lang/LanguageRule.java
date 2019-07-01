package eu.modernmt.lang;

class LanguageRule {

    private final Language pattern;
    private final Language outputLanguage;

    public LanguageRule(Language pattern, Language outputLanguage) {
        this.pattern = pattern;
        this.outputLanguage = outputLanguage;
    }

    public String getLanguage() {
        return pattern.getLanguage();
    }

    public boolean match(Language language) {
        return pattern.isEqualOrMoreGenericThan(language);
    }

    public Language getOutputLanguage() {
        return outputLanguage;
    }

}
