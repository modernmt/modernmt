package eu.modernmt.decoder.neural.queue;

import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

public class EchoPythonDecoder implements PythonDecoder {

    public static final EchoPythonDecoder INSTANCE = new EchoPythonDecoder();

    @Override
    public int getGPU() {
        return -1;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public Translation translate(LanguageDirection direction, Sentence sentence, int nBest) {
        return translate(direction, sentence, null, nBest);
    }

    @Override
    public Translation translate(LanguageDirection direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) {
        if (suggestions != null && suggestions.length > 0)
            return Translation.fromTokens(sentence, suggestions[0].translationTokens);
        else
            return Translation.fromTokens(sentence, TokensOutputStream.tokens(sentence, false, true));
    }

    @Override
    public Translation[] translate(LanguageDirection direction, Sentence[] sentences, int nBest) {
        Translation[] result = new Translation[sentences.length];
        for (int i = 0; i < result.length; i++)
            result[i] = translate(direction, sentences[i], null, nBest);
        return result;
    }

    @Override
    public Translation[] translate(LanguageDirection direction, Sentence[] sentences, ScoreEntry[] suggestions, int nBest) {
        if (sentences.length > 1 && suggestions != null && suggestions.length > 0)
            throw new UnsupportedOperationException("Echo server does not support batching with suggestions");

        Translation[] result = new Translation[sentences.length];
        for (int i = 0; i < result.length; i++)
            result[i] = translate(direction, sentences[i], suggestions, nBest);
        return result;
    }

    @Override
    public Translation align(LanguageDirection direction, Sentence sentence, String[] translation) {
        return Translation.fromTokens(sentence, translation);
    }

    @Override
    public Translation[] align(LanguageDirection direction, Sentence[] sentences, String[][] translations) {
        Translation[] result = new Translation[sentences.length];
        for (int i = 0; i < result.length; i++)
            result[i] = align(direction, sentences[i], translations[i]);
        return result;
    }

    @Override
    public void test() {
        // nothing to do
    }

    @Override
    public void close() {
        // nothing to do
    }

}
