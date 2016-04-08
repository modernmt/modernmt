package eu.modernmt.model.xmessage;

import java.util.ArrayList;

/**
 * Created by davide on 07/04/16.
 */
public class XChoiceFormat extends XFormat {

    public final Choice[] choices;

    private static final int KEY_STATE = 0;
    private static final int VALUE_STATE = 1;

    public static XChoiceFormat parse(int id, String index, String type, String modifier) throws XMessageFormatException {
        ArrayList<Choice> choices = new ArrayList<>();
        char[] chars = modifier.toCharArray();

        int state = KEY_STATE;
        int keyStart = 0;
        int valueStart = 0;

        int parenthesesCount = 0;

        String key = null;
        char divider = '\0';

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '{') {
                parenthesesCount++;
            } else if (c == '}') {
                parenthesesCount--;
            }

            if (parenthesesCount < 0)
                throw new XMessageFormatException("Unmatched braces in the pattern: " + modifier);

            if (parenthesesCount != 0)
                continue;

            if (state == KEY_STATE) {
                if (c == '<' || c == '#' || c == '\u2264' || c == '+') {
                    divider = c;
                    key = new String(chars, keyStart, i - keyStart);
                    state = VALUE_STATE;
                    valueStart = i + 1;
                }
            } else {
                if (c == '|') {
                    String pattern = new String(chars, valueStart, i - valueStart);
                    choices.add(new Choice(key, divider, XMessage.parse(pattern)));

                    state = KEY_STATE;
                    keyStart = i + 1;
                }
            }
        }

        if (state == KEY_STATE)
            throw new XMessageFormatException("Invalid modifier for type '" + type + "': " + modifier);

        String pattern = new String(chars, valueStart, chars.length - valueStart);
        choices.add(new Choice(key, divider, XMessage.parse(pattern)));

        return new XChoiceFormat(id, index, type, choices.toArray(new Choice[choices.size()]));
    }

    private XChoiceFormat(int id, String index, String type, Choice[] choices) {
        super(id, index, type, null);
        this.choices = choices;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('{');
        result.append(index);
        result.append(',');
        result.append(type);

        for (int i = 0; i < choices.length; i++) {
            Choice choice = choices[i];

            result.append(choice.key);
            result.append('#');
            result.append(choice.value);

            if (i < choices.length - 1)
                result.append('|');
        }

        if (modifier != null) {
            result.append(',');
            result.append(modifier);
        }

        result.append('}');
        return result.toString();
    }

    public static class Choice {

        public final String key;
        public final char divider;
        public final XMessage value;

        public Choice(String key, char divider, XMessage value) {
            this.key = key;
            this.divider = divider;
            this.value = value;
        }
    }

}
