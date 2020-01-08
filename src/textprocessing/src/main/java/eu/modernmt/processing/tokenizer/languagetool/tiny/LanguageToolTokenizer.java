package eu.modernmt.processing.tokenizer.languagetool.tiny;

import java.util.List;

public interface LanguageToolTokenizer {

    List<String> tokenize(String text);

}
