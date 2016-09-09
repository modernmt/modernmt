package eu.modernmt.rest.framework;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import eu.modernmt.rest.framework.routing.RouteTemplate;

public class Parameters {

    protected final RouteTemplate template;
    protected final RESTRequest req;

    public Parameters(RESTRequest req) throws ParameterParsingException {
        this.template = req.getTemplate();
        this.req = req;
    }

    public Boolean getBoolean(String name) throws ParameterParsingException {
        Boolean result = getBoolean(name, null);
        if (result == null)
            throw new ParameterParsingException(name);

        return result;
    }

    public Boolean getBoolean(String name, Boolean def) throws ParameterParsingException {
        String value = req.getParameter(name);
        if (value == null)
            return def;

        if (value.equals("true") || value.equals("1"))
            return true;
        else if (value.equals("false") || value.equals("0"))
            return false;
        else
            throw new ParameterParsingException(name, value);
    }

    public Integer getInt(String name) throws ParameterParsingException {
        Integer result = getInt(name, null);
        if (result == null)
            throw new ParameterParsingException(name);

        return result;
    }

    public Integer getInt(String name, Integer def) throws ParameterParsingException {
        String value = req.getParameter(name);
        if (value == null)
            return def;

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ParameterParsingException(name, value);
        }
    }

    public Long getLong(String name) throws ParameterParsingException {
        Long result = getLong(name, null);
        if (result == null)
            throw new ParameterParsingException(name);

        return result;
    }

    public Long getLong(String name, Long def) throws ParameterParsingException {
        String value = req.getParameter(name);
        if (value == null)
            return def;

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ParameterParsingException(name, value);
        }
    }

    public Double getDouble(String name) throws ParameterParsingException {
        Double result = getDouble(name, null);
        if (result == null)
            throw new ParameterParsingException(name);

        return result;
    }

    public Double getDouble(String name, Double def) throws ParameterParsingException {
        String value = req.getParameter(name);
        if (value == null)
            return def;

        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ParameterParsingException(name, value);
        }
    }

    public String getString(String name, boolean canBeEmpty) throws ParameterParsingException {
        String result = getString(name, canBeEmpty, null);
        if (result == null)
            throw new ParameterParsingException(name);

        return result;
    }

    public String getString(String name, boolean canBeEmpty, String def) throws ParameterParsingException {
        String value = req.getParameter(name);
        if (value == null)
            return def;

        if (!canBeEmpty && value.trim().isEmpty())
            throw new ParameterParsingException(name, value);

        return value;
    }

    public JsonObject getJSONObject(String name) throws ParameterParsingException {
        JsonObject result = getJSONObject(name, null);
        if (result == null)
            throw new ParameterParsingException(name);

        return result;
    }

    public JsonObject getJSONObject(String name, JsonObject def) throws ParameterParsingException {
        String value = req.getParameter(name);
        if (value == null)
            return def;

        try {
            JsonParser parser = new JsonParser();
            return parser.parse(value).getAsJsonObject();
        } catch (JsonParseException e) {
            throw new ParameterParsingException(name, value);
        }
    }

    public JsonArray getJSONArray(String name) throws ParameterParsingException {
        JsonArray result = getJSONArray(name, null);
        if (result == null)
            throw new ParameterParsingException(name);

        return result;
    }

    public JsonArray getJSONArray(String name, JsonArray def) throws ParameterParsingException {
        String value = req.getParameter(name);
        if (value == null)
            return def;

        try {
            JsonParser parser = new JsonParser();
            return parser.parse(value).getAsJsonArray();
        } catch (JsonParseException e) {
            throw new ParameterParsingException(name, value);
        }
    }

    public <T extends Enum<T>> T getEnum(String name, Class<T> clazz) throws ParameterParsingException {
        T result = getEnum(name, clazz, null);
        if (result == null)
            throw new ParameterParsingException(name);

        return result;
    }

    public <T extends Enum<T>> T getEnum(String name, Class<T> clazz, T def) throws ParameterParsingException {
        String value = req.getParameter(name);
        if (value == null)
            return def;

        try {
            return Enum.valueOf(clazz, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParameterParsingException(name, value);
        }
    }

    public static class ParameterParsingException extends Exception {

        public ParameterParsingException() {
            super("Missing or malformed request body");
        }

        public ParameterParsingException(Throwable cause) {
            super("Missing or malformed request body: " + cause.getMessage(), cause);
        }

        public ParameterParsingException(String param) {
            super("Missing parameter " + param);
        }

        public ParameterParsingException(String param, String value) {
            super("Invalid value for parameter " + param + ": '" + value + "'");
        }

        public ParameterParsingException(String param, Throwable cause) {
            super("Missing parameter " + param, cause);
        }

        public ParameterParsingException(String param, String value, Throwable cause) {
            super("Invalid value for parameter " + param + ": '" + value + "'", cause);
        }

    }

}
