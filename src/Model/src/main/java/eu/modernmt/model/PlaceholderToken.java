package eu.modernmt.model;

/**
 * Created by davide on 02/03/16.
 */
public abstract class PlaceholderToken extends Token {

    protected String placeholder;

    public PlaceholderToken(String text, String placeholder) {
        super(text);
        this.placeholder = placeholder;
    }

    public PlaceholderToken(String text, String placeholder, boolean rightSpace) {
        super(text, rightSpace);
        this.placeholder = placeholder;
    }

    public PlaceholderToken(String placeholder) {
        super(null);
        this.placeholder = placeholder;
    }

    public PlaceholderToken(String placeholder, boolean rightSpace) {
        super(null, rightSpace);
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public abstract void applyTransformation(PlaceholderToken sourceToken);

    public void applyFallbackTransformation() {
        this.text = this.placeholder;
    }

    public boolean hasText() {
        return text != null;
    }

    @Override
    public String getText() {
        if (text == null)
            throw new IllegalStateException("Text has not been set for Placeholder");

        return super.getText();
    }
}
