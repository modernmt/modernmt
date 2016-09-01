package eu.modernmt.rest.framework.actions;

import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.RESTResponse;

public interface Action {

    void execute(RESTRequest request, RESTResponse response);

}
