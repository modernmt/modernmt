package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.BilingualCorpusFilter;
import eu.modernmt.model.corpus.BilingualCorpus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 17/11/16.
 */
public class RareNgramFilter implements BilingualCorpusFilter {

    private final boolean useSource;
    private final HashMap<String, Integer> vocabulary = new HashMap<>();

    private double avg = -1;
    private double stddev = -1;

    public RareNgramFilter(boolean useSource) {
        this.useSource = useSource;
    }

    private static String normalize(String line) {
        return line.toLowerCase().replaceAll("\\s+", " ").replaceAll("[0-9]", "0").trim();
    }

    private static String[] tokenize(String string) {
        int length = string.length();
        if (length < 3)
            return new String[0];

        int size = length - length % 3 - 2;

        String[] result = new String[size];

        if (size > 0) {
            for (int i = 0; i < size; i++)
                result[i] = string.substring(i, i + 3);
        }

        return result;
    }

    private void computeStats() {
        double sum = 0;
        double sum2 = 0;

        for (Map.Entry<String, Integer> entry : vocabulary.entrySet()) {
            int value = entry.getValue();
            sum += value;
            sum2 += value * value;
        }

        int size = vocabulary.size();
        avg = sum / size;
        stddev = Math.sqrt((sum2 / size) - (avg * avg));
    }

    @Override
    public FilterInitializer getInitializer() {
        avg = stddev = -1;
        vocabulary.clear();

        return (corpus, pair) -> {
            String line = normalize(useSource ? pair.source : pair.target);

            for (String token : tokenize(line)) {
                Integer count = vocabulary.get(token);
                vocabulary.put(token, count == null ? 1 : count + 1);
            }
        };
    }

    @Override
    public boolean accept(BilingualCorpus.StringPair pair) throws IOException {
        if (avg < 0 || stddev < 0)
            computeStats();

        int rare = 0;
        int common = 0;

        String line = normalize(useSource ? pair.source : pair.target);

        if (line.length() < 10)
            return true;

        for (String token : tokenize(line)) {
            int count = vocabulary.containsKey(token) ? vocabulary.get(token) : 0;

            if (count > avg)
                common++;
            else
                rare++;
        }

        return common >= rare;
    }

}
