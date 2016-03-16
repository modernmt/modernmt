package eu.modernmt.processing.xml;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.tokenizer.TokenizedString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * Created by davide on 08/03/16.
 */
public class XMLSentenceBuilder implements TextProcessor<TokenizedString, Sentence> {

    @Override
    public Sentence call(TokenizedString param) throws ProcessingException {
        TokenizedString.XMLTagHook[] hooks = param.hooks;
        BitSet bits = param.bits;
        BitSet tagsBits = new BitSet(bits.length());
        char[] chars = param.string.toCharArray();

        // Ensure space char added as XML tag placeholder is tokenized.
        // Remove fake space in order to keep right information about right-space.
        for (TokenizedString.XMLTagHook hook : hooks) {
            chars[hook.position] = 'X';
            bits.set(hook.position);
            bits.set(hook.position + 1);
            tagsBits.set(hook.position + 1);
        }

        ArrayList<Token> tokens = new ArrayList<>();
        ArrayList<Tag> tags = new ArrayList<>();

        int hookIndex = 0;

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < chars.length + 1; i++) {
            if (i == chars.length || bits.get(i)) {
                if (!tagsBits.get(i)) {
                    String text = builder.toString().trim();

                    if (!text.isEmpty()) {
                        boolean rightSpace = i < chars.length && chars[i] == ' ';
                        Token token = new Token(text, rightSpace);
                        tokens.add(token);
                    }
                }

                while (hookIndex < hooks.length && hooks[hookIndex].position == i) {
                    Tag tag = hooks[hookIndex++].tag;
                    tag.setPosition(tokens.size());
                    tags.add(tag);
                }

                builder.setLength(0);
            }

            if (i < chars.length) {
                char c = chars[i];
                switch (c) {
                    case '"':
                        builder.append("&quot;");
                        break;
                    case '&':
                        builder.append("&amp;");
                        break;
                    case '\'':
                        builder.append("&apos;");
                        break;
                    case '<':
                        builder.append("&lt;");
                        break;
                    case '>':
                        builder.append("&gt;");
                        break;
                    default:
                        builder.append(c);
                        break;
                }
            }
        }

        return new Sentence(tokens.toArray(new Token[tokens.size()]), tags.toArray(new Tag[tags.size()]));
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }

}
