package eu.modernmt.processing.tokenizer.languagetool.tiny;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EsperantoWordTokenizer extends WordTokenizer {
    public EsperantoWordTokenizer() {
    }

    public List<String> tokenize(String text) {
        String replaced = text.replaceAll("(?<!')\\b([a-zA-ZĉĝĥĵŝŭĈĜĤĴŜŬ]+)'(?![a-zA-ZĉĝĥĵŝŭĈĜĤĴŜŬ-])", "$1\u0001\u0001EO_APOS1\u0001\u0001").replaceAll("(?<!')\\b([a-zA-ZĉĝĥĵŝŭĈĜĤĴŜŬ]+)'(?=[a-zA-ZĉĝĥĵŝŭĈĜĤĴŜŬ-])", "$1\u0001\u0001EO_APOS2\u0001\u0001 ");
        List<String> tokenList = super.tokenize(replaced);
        List<String> tokens = new ArrayList<>();
        Iterator itr = tokenList.iterator();

        while (itr.hasNext()) {
            String word = (String) itr.next();
            if (word.endsWith("\u0001\u0001EO_APOS2\u0001\u0001")) {
                itr.next();
            }

            word = word.replace("\u0001\u0001EO_APOS1\u0001\u0001", "'").replace("\u0001\u0001EO_APOS2\u0001\u0001", "'");
            tokens.add(word);
        }

        return tokens;
    }
}

