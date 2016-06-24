package eu.modernmt.aligner.fastalign;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Created by lucamastrostefano on 15/03/16.
 */
public class FastAlign implements Aligner {

    private static final Logger logger = LogManager.getLogger(FastAlign.class);

    static {
        try {
            logger.info("Loading jnifastalign library");
            System.loadLibrary("jnifastalign");
            logger.info("Library jnifastalign loaded successfully");
        } catch (Throwable e) {
            logger.error("Unable to load library jnifastalign", e);
            throw e;
        }
    }

    private File model;
    private boolean reverse;
    private long nativeHandle;

    FastAlign(File model, boolean reverse) {
        this.reverse = reverse;
        this.model = model;

//        if (reverse) {
//            this.command = new String[]{fastAlignPath, "-d", "-v", "-o", "-B", "-f", model.getAbsolutePath(),
//                    "-n", "1", "-b", "0", "-r"};
//        } else {
//            this.command = new String[]{fastAlignPath, "-d", "-v", "-o", "-B", "-f", model.getAbsolutePath(),
//                    "-n", "1", "-b", "0"};
//        }
    }

    private native void init(String modelFile, boolean reverse);

    @Override
    public void load() throws AlignerException {
        if (!model.isFile())
            throw new AlignerException("Invalid model path: " + model);

        this.init(model.getAbsolutePath(), reverse);
    }

    @Override
    public int[][] getAlignments(Sentence sentence, Sentence translation) throws AlignerException {
        String source = serialize(sentence.getWords());
        String target = serialize(translation.getWords());

        String encodedResult = align(source, target);
        System.out.println();
        System.out.println(encodedResult);
        System.out.println();

        return new int[0][2];
    }

    private native String align(String source, String target);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    @Override
    public void close() {
        dispose();
    }

    private native void dispose();

    private static String serialize(Word[] words) {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            text.append(words[i].getPlaceholder());

            if (i < words.length - 1)
                text.append(' ');
        }

        return text.toString();
    }

}
