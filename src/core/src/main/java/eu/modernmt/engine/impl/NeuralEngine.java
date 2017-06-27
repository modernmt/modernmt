package eu.modernmt.engine.impl;

import eu.modernmt.config.DecoderConfig;
import eu.modernmt.config.EngineConfig;
import eu.modernmt.data.DataListener;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.opennmt.OpenNMTDecoder;
import eu.modernmt.engine.Engine;
import eu.modernmt.persistence.PersistenceException;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by davide on 22/05/17.
 */
public class NeuralEngine extends Engine {

    private final OpenNMTDecoder decoder;

    public NeuralEngine(EngineConfig config) throws DecoderException, PersistenceException, IOException {
        super(config);

        DecoderConfig decoderConfig = config.getDecoderConfig();
        if (decoderConfig.isEnabled())
            this.decoder = new OpenNMTDecoder(new File(this.models, "decoder"));
        else
            this.decoder = null;
    }

    @Override
    public Decoder getDecoder() {
        if (decoder == null)
            throw new UnsupportedOperationException("Decoder unavailable");

        return decoder;
    }

    @Override
    public Collection<DataListener> getDataListeners() {
        Collection<DataListener> listeners = super.getDataListeners();
        listeners.addAll(this.decoder.getDataListeners());
        return listeners;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(decoder);
        super.close();
    }

}
