package eu.modernmt.processing.string;

import eu.modernmt.model.Tag;
import eu.modernmt.model.Word;

import java.util.List;

/**
 * Created by andrea on 28/02/17.
 * <p>
 * A tokenSet is a group of tokens
 * usually generated from a list of tokenizable transformations
 * and grouped into lists on the basis of their specific subtype
 */
public class TokenSet {
    /*List of Word Tokens*/
    public final List<Word> words;
    /*List of Tag Tokens*/
    public final List<Tag> tags;

    /**
     * Constructor that initializes a TokenSet
     * on the basis of the various lists of specific type tokens
     * generated during tokenization.
     *
     * @param words Lisf of Word tokens generated during tokenization
     * @param tags  List of Tag Tokens generated during tokenization
     */
    public TokenSet(List<Word> words, List<Tag> tags) {
        this.words = words;
        this.tags = tags;
    }
}