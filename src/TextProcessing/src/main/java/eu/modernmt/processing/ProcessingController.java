package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.processing.detokenizer.Detokenizers;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.ProcessingPipeline;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;
import eu.modernmt.processing.tokenizer.Tokenizers;
import eu.modernmt.processing.util.Splitter;
import eu.modernmt.processing.util.StringNormalizer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by davide on 27/01/16.
 */
public class ProcessingController {

    public static ProcessingPipeline<String, AnnotatedString> getTokenizePipeline(Locale language) {
        return new ProcessingPipeline.Builder<String, String>()
                .add(new Splitter())
                .add(Detokenizers.forLanguage(language))
                .add(Tokenizers.forLanguage(language))
                .create();
    }

    public static ProcessingPipeline<String[], String> getDetokenizePipeline(Locale language) {
        return new ProcessingPipeline.Builder<String[], String[]>()
                .add(Detokenizers.forLanguage(language))
                .create();
    }

    public static BitSet tokenize(String string) throws ProcessingException {
        ProcessingPipeline<String, AnnotatedString> tokenizer = getTokenizePipeline(Locale.ENGLISH);
        AnnotatedString tokens = tokenizer.process(string);
        tokenizer.close();

        return tokens.bits;
    }

    public static void main(String[] args) throws Throwable {
        System.setProperty("mmt.home", "/Users/davide/workspaces/mmt/ModernMT/");

        String string = "            </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"header\">\n" +
                "        <h3><a href=\"//stackexchange.com/sites\">more stack exchange communities</a></h3>\n" +
                "        <a href=\"http://blog.stackoverflow.com\" class=\"fr\">company blog</a>\n" +
                "    </div>\n" +
                "    <div class=\"modal-content\">\n" +
                "            <div class=\"child-content\"></div>\n" +
                "    </div>\n";

        // String string = "<c>Ciao <b> <d/>Davide </e> </b>!";

        string = new StringNormalizer().call(string);
        BitSet boundaries = tokenize(string);

        print(string, boundaries);


        Matcher m = Tag.TagRegex.matcher(string);
        int stringIndex = 0;

        ArrayList<Token> tokens = new ArrayList<>();
        ArrayList<Tag> tags = new ArrayList<>();

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            if (stringIndex < start)
                extractTokens(string, boundaries, stringIndex, start, tokens);

            stringIndex = end;

            Tag tag = Tag.fromText(m.group());
            tag.setLeftSpace(start > 0 && string.charAt(start - 1) == ' ');
            tag.setRightSpace(end < string.length() && string.charAt(end) == ' ');
            tag.setPosition(tokens.size());
            tags.add(tag);
        }

        if (stringIndex < string.length())
            extractTokens(string, boundaries, stringIndex, string.length(), tokens);

        // print(string, boundaries);
        System.out.println(tokens);
        System.out.println(tags);

        Sentence sentence = new Sentence(tokens.toArray(new Token[tokens.size()]), tags.toArray(new Tag[tags.size()]));
        System.out.println(sentence);

        System.out.println(sentence.toString().replace(" ", "").equals(string.replace(" ", "")));
    }

    private static void extractTokens(String string, BitSet boundaries, int start, int end, ArrayList<Token> output) {
        int length = boundaries.length();

        for (int i = start; i < end + 1; i++) {
            if (end == length || boundaries.get(i)) {
                String text = string.substring(start, i).trim();

                if (!text.isEmpty()) {
                    Token token = new Token(string.substring(start, i), true);
                    output.add(token);
                }

                start = i;
            }
        }
    }

    private static void print(String string, BitSet boundaries) {
        System.out.println(string);
        for (int i = 0; i < string.length(); i++) {
            System.out.print(boundaries.get(i) ? '1' : '0');
        }
        System.out.println();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            boolean bound = boundaries.get(i);
            if (bound)
                System.out.print('|');
            System.out.print(c);
        }
        System.out.println('\n');
    }
}
