package eu.modernmt.decoder.moses;

import eu.modernmt.decoder.Decoder;

import java.io.File;

/**
 * Created by davide on 26/11/15.
 */
public class MosesDecoder implements Decoder {

    static {
        System.loadLibrary("jnimoses");
    }

    private long nativeHandle;

    public MosesDecoder(File mosesIni) {
        init(mosesIni.getAbsolutePath());
    }

    private native void init(String mosesIni);

    @Override
    public native String translate(String text);

    public native void dispose();

    @Override
    protected void finalize() throws Throwable {
        dispose();
    }

    public static void main(String[] args) {
        MosesDecoder decoder = new MosesDecoder(new File("."));
        System.out.println(decoder.translate("Hello world"));
    }

}
