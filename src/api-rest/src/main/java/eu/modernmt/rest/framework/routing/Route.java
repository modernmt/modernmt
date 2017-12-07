package eu.modernmt.rest.framework.routing;

import eu.modernmt.rest.framework.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by davide on 15/12/15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Route {

    String[] aliases();

    HttpMethod method();

    boolean log() default true;

}

