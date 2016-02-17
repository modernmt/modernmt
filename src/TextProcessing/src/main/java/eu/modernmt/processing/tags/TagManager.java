package eu.modernmt.processing.tags;

/**
 * Created by davide on 17/02/16.
 */
public class TagManager {

    public void remap(_Sentence source, _Translation translation) {
        // TODO:
        // translation.setTags(...);
    }

    public static void main(String[] args) throws Throwable {
        _Sentence source = new _Sentence(new _Token[] {
                new _Token("Ciao", true),
                new _Token("Davide", false),
                new _Token("!", false),
        }, new _Tag[] {
                new _Tag("<b>", true, false, 1),
                new _Tag("</b>", false, false, 2),
        });

        _Translation translation = new _Translation(new _Token[] {
                new _Token("Hello", true),
                new _Token("Davide", false),
                new _Token("!", false),
        }, source, new int[][] {
                {0, 0},
                {1, 1},
                {2, 2},
        });

        new TagManager().remap(source, translation);

        System.out.println(source);
        System.out.println(source.getStrippedString());
        System.out.println();
        System.out.println(translation);
        System.out.println(translation.getStrippedString());
    }
}
