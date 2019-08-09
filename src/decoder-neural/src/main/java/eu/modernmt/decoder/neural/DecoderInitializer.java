package eu.modernmt.decoder.neural;

import eu.modernmt.config.DecoderConfig;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.queue.DecoderQueue;
import eu.modernmt.decoder.neural.scheduler.Scheduler;
import eu.modernmt.memory.TranslationMemory;

import java.io.File;
import java.io.IOException;

public interface DecoderInitializer {

    ModelConfig createModelConfig(File filepath) throws IOException;

    TranslationMemory createTranslationMemory(DecoderConfig config, ModelConfig modelConfig, File model) throws IOException;

    DecoderQueue createDecoderQueue(DecoderConfig config, ModelConfig modelConfig, File model) throws DecoderException;

    Scheduler createScheduler(DecoderConfig config, ModelConfig modelConfig, int queueSize) throws DecoderException;

    DecoderExecutor createDecoderExecutor(DecoderConfig config, ModelConfig modelConfig) throws DecoderException;

}
