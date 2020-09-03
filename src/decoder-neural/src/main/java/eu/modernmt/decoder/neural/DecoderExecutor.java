package eu.modernmt.decoder.neural;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.queue.PythonDecoder;
import eu.modernmt.decoder.neural.scheduler.TranslationSplit;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;

import java.util.Collection;
import java.util.List;

public interface DecoderExecutor {

    void align(PythonDecoder decoder, LanguageDirection language, List<TranslationSplit> splits) throws DecoderException;

    void translate(PythonDecoder decoder, LanguageDirection language, List<TranslationSplit> splits, Collection<ScoreEntry> suggestions) throws DecoderException;

}
