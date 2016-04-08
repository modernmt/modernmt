package eu.modernmt.model.xmessage;

import java.util.regex.Pattern;

/**
 * Created by davide on 07/04/16.
 */
public class XFormat {

    public static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{[0-9]+(,[a-z]+)?\\}");

    public final int id;
    public final String index;
    public final String type;
    public final String modifier;

    private String placeholder;

    public XFormat(int id, String index, String type, String modifier) {
        this.id = id;
        this.index = index;
        this.type = type;
        this.modifier = modifier;
    }

    public final String getPlaceholder() {
        if (this.placeholder == null) {
            StringBuilder placeholder = new StringBuilder();
            placeholder.append('{');
            placeholder.append(id);

            if (type != null) {
                placeholder.append(',');
                placeholder.append(type);
            }

            placeholder.append('}');
            this.placeholder = placeholder.toString();

        }

        return this.placeholder;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('{');
        result.append(index);

        if (type != null) {
            result.append(',');
            result.append(type);
        }

        if (modifier != null) {
            result.append(',');
            result.append(modifier);
        }

        result.append('}');
        return result.toString();
    }
}
