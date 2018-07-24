package eu.modernmt.api.framework.actions;

import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;

public abstract class CollectionAction<M> extends JSONAction {

    @Override
    @SuppressWarnings("unchecked")
    protected final JSONActionResult getResult(RESTRequest req, Parameters params) throws Throwable {
        Collection<M> collection = execute(req, params);
        Class<M> objectClass = (Class<M>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        return collection == null ? null : new CollectionActionResult<>(collection, objectClass);
    }

    protected abstract Collection<M> execute(RESTRequest req, Parameters params) throws Throwable;

}
