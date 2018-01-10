package eu.modernmt.engine.impl;

import eu.modernmt.config.EngineConfig;
import eu.modernmt.config.NeuralDecoderConfig;
import eu.modernmt.data.DataListener;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.neural.NeuralDecoder;
import eu.modernmt.decoder.neural.NeuralDecoderException;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.ContributionOptions;
import eu.modernmt.engine.Engine;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.util.Collection;

/**
 * Created by davide on 22/05/17.
 */
public class NeuralEngine extends Engine {

    private static final ContributionOptions CONTRIBUTION_OPTIONS = new ContributionOptions(true, true);
    private final NeuralDecoder decoder;

    public NeuralEngine(EngineConfig config) throws BootstrapException {
        super(config);

        try {
            NeuralDecoderConfig decoderConfig = (NeuralDecoderConfig) config.getDecoderConfig();
            if (decoderConfig.isEnabled()) {
                this.decoder = decoderConfig.isUsingGPUs() ?
                        new NeuralDecoder(new File(this.models, "decoder"), decoderConfig.getGPUs()) :
                        new NeuralDecoder(new File(this.models, "decoder"), decoderConfig.getThreads());
            } else {
                this.decoder = null;
            }
        } catch (NeuralDecoderException e) {
            throw new BootstrapException("Failed to instantiate NMT Decoder", e);
        }
    }

    @Override
    public ContributionOptions getContributionOptions() {
        return CONTRIBUTION_OPTIONS;
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
