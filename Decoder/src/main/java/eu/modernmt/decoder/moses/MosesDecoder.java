package eu.modernmt.decoder.moses;

import eu.modernmt.decoder.*;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.context.ContextDocument;
import eu.modernmt.model.context.TranslationContext;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by davide on 26/11/15.
 */
public class MosesDecoder implements Decoder {

    static {
        System.loadLibrary("jnimoses");
    }

    private long nativeHandle;
    private File inifile;

    public MosesDecoder(File inifile) {
        this.inifile = inifile;
        this.init(inifile.getAbsolutePath());
    }

    private native void init(String mosesIni);

    @Override
    public native List<DecoderFeature> getFeatureWeights();

    @Override
    public void setFeatureWeights(List<DecoderFeature> features) {
        // TODO:
        throw new UnsupportedOperationException();
    }

    @Override
    public DecoderSession openSession(TranslationContext translationContext) {
        long sessionId = this.createSession(context2map(translationContext));
        return new MosesSession(this, sessionId, translationContext);
    }

    @Override
    public Translation translate(Sentence text) {
        return translate(text.toString(), 0L, null, 0);
    }

    @Override
    public Translation translate(Sentence text, TranslationContext translationContext) {
        return translate(text.toString(), 0L, context2map(translationContext), 0);
    }

    @Override
    public Translation translate(Sentence text, DecoderSession session) {
        return translate(text.toString(), session.getId(), null, 0);
    }

    @Override
    public Translation translate(Sentence text, DecoderSession session, int nbestListSize) {
        return translate(text.toString(), session.getId(), null, nbestListSize);
    }

    protected native long createSession(Map<String, Float> translationContext);

    protected native void closeSession(long session);

    protected native Translation translate(String text, long session, Map<String, Float> translationContext, int nbestListSize);

    protected native void dispose();

    private Map<String, Float> context2map(TranslationContext translationContext) {
        if (translationContext == null)
            return null;

        HashMap<String, Float> map = new HashMap<>();
        for (ContextDocument document : translationContext.getDocuments()) {
            map.put(document.getId(), translationContext.getScore(document));
        }

        return map;
    }


    @Override
    protected void finalize() throws Throwable {
        dispose();
    }

    @Override
    public void close() throws IOException {
        dispose();
    }

}
