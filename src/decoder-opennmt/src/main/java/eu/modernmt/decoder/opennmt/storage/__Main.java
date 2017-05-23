package eu.modernmt.decoder.opennmt.storage;

import eu.modernmt.decoder.opennmt.storage.lucene.LuceneTranslationsStorage;
import eu.modernmt.model.Domain;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpora;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by davide on 23/05/17.
 */
public class __Main {

    private static final File path = new File("/Users/davide/Desktop/lucene-test");

    public static void main(String[] args) throws Throwable {
        query("However, Jacques Santer is not to blame for that, as with the best will in the world he cannot intervene in his Commissioners' individual departments.");
    }

    public static void query(String text) throws Throwable {
        String[] tokens = text.split(" ");
        Word[] words = new Word[tokens.length];

        for (int i = 0; i < tokens.length; i++)
            words[i] = new Word(tokens[i], " ");

        Sentence s = new Sentence(words);

        LuceneTranslationsStorage storage = new LuceneTranslationsStorage(path);
        TranslationsStorage.SearchResult result = storage.search(s, 10);

        for (int i = 0; i < result.size(); i++) {
            TranslationsStorage.Entry entry = result.entries[i];
            System.out.println("<" + entry.domain + ", " + StringUtils.join(entry.sentence, ' ') +
                    ", " + StringUtils.join(entry.translation, ' ') + "> " + result.scores[i]);
        }
    }

    public static void create() throws Throwable {
        if (path.isDirectory())
            FileUtils.forceDelete(path);

        LuceneTranslationsStorage storage = new LuceneTranslationsStorage(path);

        ArrayList<BilingualCorpus> corpora = new ArrayList<>();
        Corpora.list(null, true, corpora, Locale.ENGLISH, Locale.ITALIAN, new File("/Users/davide/workspaces/mmt/ModernMT/examples/data/train"));

        int id = 1;
        for (BilingualCorpus corpus : corpora) {
            storage.add(new Domain(id++), corpus);
        }
    }
}
