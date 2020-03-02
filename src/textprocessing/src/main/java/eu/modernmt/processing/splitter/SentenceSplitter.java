package eu.modernmt.processing.splitter;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Word;

import java.util.ArrayList;
import java.util.List;

public class SentenceSplitter {

    private static int count(Sentence sentence) {
        Word[] words = sentence.getWords();

        int result = 0;
        for (int i = 0; i < words.length; i++) {
            if (words[i].isSentenceBreak() || i == words.length - 1)
                result++;
        }

        return result;
    }

    public static List<Sentence> split(Sentence sentence) {
        return split(sentence, false);
    }

    public static List<Sentence> split(Sentence sentence, boolean includeTags) {
        return split(sentence, includeTags, 4);
    }

    public static List<Sentence> split(Sentence sentence, boolean includeTags, int minLength) {
        ArrayList<Sentence> output = new ArrayList<>(count(sentence));

        boolean pendingSentenceBreak = false;
        Builder builder = new Builder(includeTags);

        for (Token token : sentence) {
            if (!includeTags && (token instanceof Tag)) continue;

            if (pendingSentenceBreak) {
                if ((token instanceof Tag) && !((Tag) token).isOpeningTag()) {
                    builder.append(token);
                    continue;
                } else {
                    output.add(builder.toSentence());
                    builder.clear();
                }
            }

            builder.append(token);
            pendingSentenceBreak = token.isSentenceBreak() && builder.wordcount() >= minLength;
        }

        if (builder.size() > 0)
            output.add(builder.toSentence());

        return output;
    }

    private static class Builder {

        private final boolean includeTags;
        private final ArrayList<Word> words = new ArrayList<>();
        private final ArrayList<Tag> tags = new ArrayList<>();

        public Builder(boolean includeTags) {
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

}
