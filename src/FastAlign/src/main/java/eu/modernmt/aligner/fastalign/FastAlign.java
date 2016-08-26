package eu.modernmt.aligner.fastalign;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Created by lucamastrostefano on 15/03/16.
 */
public class FastAlign implements Aligner {

    private static final Logger logger = LogManager.getLogger(FastAlign.class);

    static {
        try {
            System.loadLibrary("fastalign");
        } catch (Throwable e) {
            logger.error("Unable to load library 'fastalign'", e);
            throw e;
        }
    }

    private File model;
    private long nativeHandle;

    FastAlign(File model) {
        this.model = model;
    }

    @Override
    public void load() throws AlignerException {
        if (!model.isFile())
            throw new AlignerException("Invalid model path: " + model);

        this.nativeHandle = instantiate(model.getAbsolutePath(), Runtime.getRuntime().availableProcessors());
    }

    private native long instantiate(String modelFile, int threads);

    @Override
    public int[][] getAlignment(Sentence sentence, Sentence translation) throws AlignerException {
        String source = serialize(sentence.getWords());
        String target = serialize(translation.getWords());

        return alignPair(source, target);
    }

    @Override
    public int[][][] getAlignments(List<Sentence> sentences, List<Sentence> translations) throws AlignerException {
        String[] sources = new String[sentences.size()];
        String[] targets = new String[translations.size()];

        Iterator<Sentence> sentenceIterator = sentences.iterator();
        Iterator<Sentence> translationIterator = translations.iterator();

        int i = 0;
        while (sentenceIterator.hasNext() && translationIterator.hasNext()) {
            sources[i] = serialize(sentenceIterator.next().getWords());
            targets[i] = serialize(translationIterator.next().getWords());
            i++;
        }

        return alignPairs(sources, targets);
    }


    private native int[][] alignPair(String source, String target);

    private native int[][][] alignPairs(String[] sources, String[] targets);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        nativeHandle = dispose(nativeHandle);
    }

    private native long dispose(long handle);

    private static String serialize(Word[] words) {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            text.append(words[i].getId());

            if (i < words.length - 1)
                text.append(' ');
        }

        return text.toString();
    }

}
