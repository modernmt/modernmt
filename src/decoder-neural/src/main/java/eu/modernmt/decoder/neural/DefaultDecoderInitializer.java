package eu.modernmt.decoder.neural;

import eu.modernmt.config.DecoderConfig;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.decoder.neural.queue.DecoderQueue;
import eu.modernmt.decoder.neural.queue.DecoderQueueImpl;
import eu.modernmt.decoder.neural.queue.PythonDecoder;
import eu.modernmt.decoder.neural.queue.PythonDecoderImpl;
import eu.modernmt.decoder.neural.scheduler.Scheduler;
import eu.modernmt.decoder.neural.scheduler.SentenceBatchScheduler;
import eu.modernmt.memory.TranslationMemory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class DefaultDecoderInitializer implements DecoderInitializer {

    private static File getJarPath() throws DecoderException {
        URL url = Decoder.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new DecoderException("Unable to resolve JAR file", e);
        }
    }

    @Override
    public ModelConfig createModelConfig(File filepath) throws IOException {
        return ModelConfig.load(filepath);
    }

    @Override
    public TranslationMemory createTranslationMemory(DecoderConfig config, ModelConfig modelConfig, File model) throws IOException {
        return new LuceneTranslationMemory(model, modelConfig.getQueryMinimumResults());
    }

    @Override
    public DecoderQueue createDecoderQueue(DecoderConfig config, ModelConfig modelConfig, File model) throws DecoderException {
        PythonDecoder.Builder builder = new PythonDecoderImpl.Builder(getJarPath(), model);

        if (config.isUsingGPUs())
            return DecoderQueueImpl.newGPUInstance(modelConfig, builder, config.getGPUs());
        else
            return DecoderQueueImpl.newCPUInstance(modelConfig, builder, config.getThreads());
    }

    @Override
    public Scheduler createScheduler(DecoderConfig config, ModelConfig modelConfig, int queueSize) {
        return new SentenceBatchScheduler(queueSize);
    }

    @Override
    public DecoderExecutor createDecoderExecutor(DecoderConfig config, ModelConfig modelConfig) {
        return new DecoderExecutorImpl();
    }

}
