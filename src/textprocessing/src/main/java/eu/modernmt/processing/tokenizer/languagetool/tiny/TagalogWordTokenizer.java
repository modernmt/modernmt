package eu.modernmt.processing.tokenizer.languagetool.tiny;

/**
 * @since 2.8
 */
public class TagalogWordTokenizer extends WordTokenizer {

    @Override
    public String getTokenizingCharacters() {
        return super.getTokenizingCharacters() + "-";
    }

}

