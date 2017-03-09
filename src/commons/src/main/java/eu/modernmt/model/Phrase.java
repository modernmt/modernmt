package eu.modernmt.model;

/**
 * Created by davide on 27/01/17.
 */
public class Phrase implements Comparable<Phrase> {

    private final int index;
    private final Word[] source;
    private final Word[] target;

    public Phrase(int index, Word[] source, Word[] target) {
        this.source = source;
        this.target = target;
        this.index = index;
    }

    public Word[] getSource() {
        return source;
    }

    public Word[] getTarget() {
        return target;
    }

    @Override
    public int compareTo(Phrase o) {
        return Integer.compare(index, o.index);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < source.length; i++) {
            if (i > 0)
                result.append(' ');
            result.append(source[i]);
        }

        result.append(" ↔ ");

        for (int i = 0; i < target.length; i++) {
            if (i > 0)
                result.append(' ');
            result.append(target[i]);
        }

        return result.toString();
    }
}
