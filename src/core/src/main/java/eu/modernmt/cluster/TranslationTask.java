package eu.modernmt.cluster;

import eu.modernmt.cluster.services.Prioritizable;
import eu.modernmt.model.Translation;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * A TranslationTask is a Callable for Translations.
 */
public interface TranslationTask extends Callable<Translation>, Serializable, Prioritizable {
 /* A TranslationTask must also extends Prioritizable, in order to define which task has higher priority.
 * Tasks can be sent across the MMT cluster, thus requiring TranslationTasks to extend Serializable too.*/

}
