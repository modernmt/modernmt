package eu.modernmt.processing.numbers.internal;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class NumericSequence implements Iterable<NumericPlaceholder> {

    private final LinkedList<NumericPlaceholder> elements;
    private final HashMap<Integer, NumericPlaceholder> index2element;

    public static NumericSequence build(Sentence sentence) {
        LinkedList<NumericPlaceholder> sequence = new LinkedList<>();
        Word[] words = sentence.getWords();

        for (int i = 0; i < words.length; i++) {
            NumericPlaceholder placeholder = NumericPlaceholder.build(i, words[i]);
            if (placeholder != null)
                sequence.add(placeholder);
        }

        return new NumericSequence(sequence);
    }

    private NumericSequence(LinkedList<NumericPlaceholder> elements) {
        this.elements = elements;

        this.index2element = new HashMap<>(elements.size());
        for (NumericPlaceholder e : elements)
            this.index2element.put(e.getIndex(), e);
    }

    public boolean hasIndex(int index) {
        return index2element.containsKey(index);
    }

    public NumericPlaceholder getByIndex(int index) {
        return index2element.get(index);
    }

    public void remove(NumericPlaceholder e) {
        this.elements.remove(e);
        this.index2element.remove(e.getIndex());
    }

    public boolean isEmpty() {
        return this.elements.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<NumericPlaceholder> iterator() {
        return new Iterator<NumericPlaceholder>() {

            private final Iterator<NumericPlaceholder> delegate = elements.iterator();
            private NumericPlaceholder e = null;

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public void remove() {
                if (e != null) {
                    delegate.remove();
                    index2element.remove(e.getIndex());
                    e = null;
                }
            }

            @Override
            public NumericPlaceholder next() {
                e = delegate.next();
                return e;
            }
        };
    }
}
