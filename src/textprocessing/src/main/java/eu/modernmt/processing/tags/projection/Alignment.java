package eu.modernmt.processing.tags.projection;

import java.util.ArrayList;
import java.util.List;

public class Alignment {

    private List<Coverage> alignment;

    Alignment(eu.modernmt.model.Alignment a, int sourceWords, int targetWords) {
        this.alignment = new ArrayList<>(sourceWords + 1);
        //create an empty Coverage for each source word; they may remain empty
        //an additional position is reserved for (sourceWords+1) which is used for tags anchored to the end of the sentence
        for (int i = 0; i < sourceWords + 1; i++) {
            this.alignment.add(new Coverage());
        }
        //populate the coverage for each source word
        for (int i = 0; i < a.getSourceIndexes().length; i++) {
            this.alignment.get(a.getSourceIndexes()[i]).add(a.getTargetIndexes()[i]);
        }
        //create an artificial alignment point between positions (sourceWords) and (targetWords) (first words after the sentence
        this.alignment.get(sourceWords).add(targetWords);
    }

    protected Coverage get(int pos) {
        return this.alignment.get(pos);
    }

    public int size() {
        return this.alignment.size();
    }

    public void print() {
        for (int i = 0; i < alignment.size(); i++) {
            Coverage coverage = alignment.get(i);
            if (!coverage.isEmpty()) {
                System.out.print(i + ":" + coverage.toString() + ", ");
            }
        }
        System.out.println();
    }
}
