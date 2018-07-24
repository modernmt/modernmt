package eu.modernmt.api.framework.routing;

import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.actions.Action;

import java.util.ArrayList;
import java.util.List;

public class RouteTemplate {

    private final String template;
    private final Class<? extends Action> actionClass;
    private final List<String> tokens;
    private final HttpMethod method;

    public RouteTemplate(String template, Class<? extends Action> actionClass,
                         HttpMethod method) {
        this.template = template;
        this.actionClass = actionClass;
        this.method = method;
        this.tokens = tokenize(template);
    }

    public static List<String> tokenize(String path) {
        List<String> tokens = new ArrayList<String>();
        for (String t : path.split("/")) {
            t = t.trim();
            if (!t.isEmpty())
                tokens.add(t);
        }

        return tokens;
    }

    public Class<? extends Action> getActionClass() {
        return actionClass;
    }

    public HttpMethod getMethod() {
        return this.method;
    }

    public int indexOfToken(String token) {
        return tokens.indexOf(token);
    }

    public String getTokenAt(int index) {
        if (index < tokens.size())
            return tokens.get(index);
        else
            return null;
    }

    public boolean isTokenVariable(String token) {
        return token.charAt(0) == ':';
    }

    public int size() {
        return this.tokens.size();
    }


//
//    public <T extends Enum<T>> T getEnum(RESTRequest req, String varName, Class<T> clazz) throws TemplateException {
//        String value = getString(req, varName);
//
//        try {
//            return (T) Enum.valueOf(clazz, value.toUpperCase());
//        } catch (IllegalArgumentException e) {
//            throw new TemplateException(':' + varName);
//        }
//    }
//
//    public <T extends Enum<T>> T getEnum(RESTRequest req, String varName, Class<T> clazz, T def) throws TemplateException {
//        try {
//            String value = getString(req, varName);
//            return (T) Enum.valueOf(clazz, value.toUpperCase());
//        } catch (Exception e) {
//            return def;
//        }
//    }
//
//    public Long getLong(RESTRequest req, String varName) throws TemplateException {
//        String value = getString(req, varName);
//        try {
//            return Long.valueOf(value);
//        } catch (Exception e) {
//            throw new TemplateException(varName);
//        }
//    }
//
//    public Long getLong(RESTRequest req, String varName, Long def) {
//        try {
//            String value = getString(req, varName);
//            return Long.valueOf(value);
//        } catch (Exception e) {
//            return def;
//        }
//    }

    @Override
    public String toString() {
        return method + " " + template + " > " + actionClass.getCanonicalName();
    }

}
