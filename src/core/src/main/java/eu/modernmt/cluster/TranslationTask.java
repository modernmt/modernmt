package eu.modernmt.cluster;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Translation;

import java.io.Serializable;
import java.util.concurrent.Callable;

public interface TranslationTask extends Callable<Translation>, Serializable {

    LanguageDirection getLanguageDirection();

}
