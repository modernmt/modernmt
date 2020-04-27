package eu.modernmt.processing.splitter;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Word;

import java.util.ArrayList;

class SentenceSplit {

    private final boolean includeTags;
    private final ArrayList<Word> words = new ArrayList<>();
    private final ArrayList<Tag> tags = new ArrayList<>();

    public SentenceSplit(boolean includeTags) {
        this.includeTags = includeTags;
    }

    public void append(Token token) {
        if (token instanceof Tag) {
            if (includeTags) {
                Tag tag = ((Tag) token).clone();
                tag.setPosition(words.size());
                tags.add(tag);
            }
        } else {
            this.words.add((Word) token);
        }
    }

    public Sentence toSentence() {
        Word[] words = this.words.toArray(new Word[0]);
        Tag[] tags = this.tags.toArray(new Tag[0]);
        return new Sentence(words, tags);
    }

    public int wordcount() {
        return words.size();
    }

    public int size() {
        return words.size() + tags.size();
    }

    public void clear() {
        words.clear();
        tags.clear();
    }
}
