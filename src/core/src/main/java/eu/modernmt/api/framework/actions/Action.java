package eu.modernmt.api.framework.actions;

import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.RESTResponse;

public interface Action {

    void execute(RESTRequest request, RESTResponse response);

}
