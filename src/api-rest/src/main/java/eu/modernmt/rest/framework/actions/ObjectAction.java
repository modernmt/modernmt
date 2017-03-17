package eu.modernmt.rest.framework.actions;

import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;

import java.lang.reflect.ParameterizedType;

public abstract class ObjectAction<M> extends JSONAction {

    @Override
    @SuppressWarnings("unchecked")
    protected final ObjectActionResult getResult(RESTRequest req, Parameters params) throws Throwable {
        M object = execute(req, params);
        Class<M> objectClass = (Class<M>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        return object == null ? null : new ObjectActionResult<>(object, objectClass);
    }

    protected abstract M execute(RESTRequest req, Parameters params) throws Throwable;

}
