package eu.modernmt.processing.tokenizer;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseTokenizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    protected final List<Annotator> annotators = new ArrayList<>();

    public BaseTokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public SentenceBuilder call(SentenceBuilder sentence, Map<String, Object> metadata) throws ProcessingException {
        TokenizedString string = new TokenizedString(sentence.toString(), true);

        for (Annotator annotator : annotators)
            annotator.annotate(string);

        return string.compile(sentence);
    }

    public interface Annotator {

        void annotate(TokenizedString string) throws ProcessingException;

    }

}
