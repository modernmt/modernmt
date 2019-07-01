package eu.modernmt.lang;

class LanguageRule {

    private final LanguagePattern pattern;
    private final Language outputLanguage;

    public LanguageRule(LanguagePattern pattern, Language outputLanguage) {
        this.pattern = pattern;
        this.outputLanguage = outputLanguage;
    }

    public String getLanguage() {
        return pattern.getLanguage();
    }

    public boolean match(Language language) {
        return pattern.match(language);
    }

    public Language getOutputLanguage() {
        return outputLanguage;
    }

}
