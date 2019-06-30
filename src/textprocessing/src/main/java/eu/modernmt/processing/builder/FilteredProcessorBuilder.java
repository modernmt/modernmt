package eu.modernmt.processing.builder;

import eu.modernmt.lang.Language2;

import java.util.HashSet;

/**
 * Created by davide on 31/05/16.
 */
class FilteredProcessorBuilder extends ProcessorBuilder {

    private final Filter sourceFilter;
    private final Filter targetFilter;

    public FilteredProcessorBuilder(String className, String sourceFilter, String targetFilter) {
        super(className);
        this.sourceFilter = parseFilter(sourceFilter);
        this.targetFilter = parseFilter(targetFilter);
    }

    private static Filter parseFilter(String definition) {
        if (definition == null)
            return null;

        definition = definition.trim();
        if (definition.length() < 2)
            return null;

        if (definition.charAt(0) == '^')
            return new NorFilter(definition.substring(1).trim().split("\\s+"));
        else
            return new OrFilter(definition.split("\\s+"));
    }

    public boolean accept(Language2 sourceLanguage, Language2 targetLanguage) {
        if (this.sourceFilter != null && !this.sourceFilter.accept(sourceLanguage))
            return false;
        if (this.targetFilter != null && !this.targetFilter.accept(targetLanguage))
            return false;
        return true;
    }

    private interface Filter {

        boolean accept(Language2 language);

    }

    private static class OrFilter implements Filter {

        private final HashSet<String> languages;

        private OrFilter(String[] languages) {
            this.languages = new HashSet<>(languages.length);

            for (String lang : languages) {
                Language2 parsed = Language2.fromString(lang);
                if (!parsed.getLanguage().equals(parsed.toLanguageTag()))
                    throw new IllegalArgumentException("Text processing framework only supports language-only tags, but found complex language: " + lang);

                this.languages.add(parsed.getLanguage());
            }
        }

        @Override
        public boolean accept(Language2 language) {
            return language != null && languages.contains(language.getLanguage());
        }
    }

    private static class NorFilter extends OrFilter {

        private NorFilter(String[] languages) {
            super(languages);
        }

        @Override
        public boolean accept(Language2 language) {
            return !super.accept(language);
        }
    }
}
