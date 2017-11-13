package eu.modernmt.rest.framework;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.rest.framework.routing.RouteTemplate;

import java.util.Locale;
import java.util.Set;

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

    public Locale getLocale(String name) throws ParameterParsingException {
        return Locale.forLanguageTag(getString(name, false));
    }

    public Locale getLocale(String name, Locale def) throws ParameterParsingException {
        String tag = getString(name, false, null);
        return tag == null ? def : Locale.forLanguageTag(tag);
    }

    public Locale[] getLocaleArray(String name) throws ParameterParsingException {
        String[] rawArray = getString(name, false).split(",");
        Locale[] array = new Locale[rawArray.length];

        for (int i = 0; i < rawArray.length; i++)
            array[i] = Locale.forLanguageTag(rawArray[i]);

        return array;
    }

    public Locale[] getLocaleArray(String name, Locale[] def) throws ParameterParsingException {
        String rawValue = getString(name, false, null);
        if (rawValue == null)
            return def;

        String[] rawArray = rawValue.split(",");
        Locale[] array = new Locale[rawArray.length];

        for (int i = 0; i < rawArray.length; i++)
            array[i] = Locale.forLanguageTag(rawArray[i]);

        return array;
    }

    /**
     * Get the language direction from the engine if the engine is multilingual, else get them from the parameters.
     *
     * @return
     * @throws ParameterParsingException
     */
    public LanguagePair getLanguagePair(String sourceName, String targetName) throws ParameterParsingException {
        Locale sourceLanguage = getLocale(sourceName, null);
        Locale targetLanguage = getLocale(targetName, null);

        if (sourceLanguage == null && targetLanguage == null) {
            Set<LanguagePair> supportedLanguages = ModernMT.getNode().getEngine().getLanguages().getLanguages();
            if (supportedLanguages.size() == 1) {
                return supportedLanguages.iterator().next();
            } else {
                throw new ParameterParsingException(sourceName);
            }
        } else if (sourceLanguage == null) {
            throw new ParameterParsingException(sourceName);
        } else if (targetLanguage == null) {
            throw new ParameterParsingException(targetName);
        } else {
            return new LanguagePair(sourceLanguage, targetLanguage);
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

        public ParameterParsingException(String param, String value, String explanation) {
            super("Invalid value for parameter " + param + ": '" + value + "' (" + explanation + ")");

        }

    }
}