package eu.modernmt.data;

import java.util.Collection;
import java.util.Map;

/**
 * Created by davide on 31/10/17.
 */
public interface DataBatch {

    /**
     * @return a collection of TranslationUnits not accepted by current LanguageIndex
     */
    Collection<TranslationUnit> getDiscardedTranslationUnits();

    Collection<TranslationUnit> getTranslationUnits();

    Collection<Deletion> getDeletions();

    Map<Short, Long> getChannelPositions();

}
