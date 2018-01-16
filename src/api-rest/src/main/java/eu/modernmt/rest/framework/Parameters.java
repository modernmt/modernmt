package eu.modernmt.rest.framework;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
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

    public Language getLanguage(String name) throws ParameterParsingException {
        return Language.fromString(getString(name, false));
    }

    public Language getLanguage(String name, Language def) throws ParameterParsingException {
        String tag = getString(name, false, null);
        return tag == null ? def : Language.fromString(tag);
    }

    public Language[] getLanguageArray(String name) throws ParameterParsingException {
        String[] rawArray = getString(name, false).split(",");
        Language[] array = new Language[rawArray.length];

        for (int i = 0; i < rawArray.length; i++)
            array[i] = Language.fromString(rawArray[i]);

        return array;
    }

    public Language[] getLanguageArray(String name, Language[] def) throws ParameterParsingException {
        String rawValue = getString(name, false, null);
        if (rawValue == null)
            return def;

        String[] rawArray = rawValue.split(",");
        Language[] array = new Language[rawArray.length];

        for (int i = 0; i < rawArray.length; i++)
            array[i] = Language.fromString(rawArray[i]);

        return array;
    }

    /**
     * Get the language pair from the request parameter using the passed source and target language parameter names.
     * <p>
     * If neither the source language parameter nor the target language parameter are passed, return a default LanguagePair.
     *
     * @param sourceName the name of the source language field to parse
     * @param targetName the name of the target language field to parse
     * @param def        the default LanguagePair to return if neither sourceName nor targetName can be parsed from the request
     * @return language pair build from the values of parameters sourceName and targetName, if they are passed, or the default LanguagePair otherwise.
     * @throws ParameterParsingException
     */
    public LanguagePair getLanguagePair(String sourceName, String targetName, LanguagePair def) throws ParameterParsingException {
        Language sourceLanguage = getLanguage(sourceName, null);
        Language targetLanguage = getLanguage(targetName, null);

        if (sourceLanguage == null && targetLanguage == null) {
            return def;
        } else if (sourceLanguage == null) {
            throw new ParameterParsingException(sourceName);
        } else if (targetLanguage == null) {
            throw new ParameterParsingException(targetName);
        } else {
            return new LanguagePair(sourceLanguage, targetLanguage);
        }
    }

    /**
     * Get the language pair from the request parameter using the passed source and target language parameter names.
     *
     * @param sourceName the name of the source language field to parse
     * @param targetName the name of the target language field to parse
     * @return language pair build from the values of parameters sourceName and targetName
     * @throws ParameterParsingException if the parameter with name sourceName and/or the parameter with name targetName can not be found in the request
     */
    public LanguagePair getLanguagePair(String sourceName, String targetName) throws ParameterParsingException {
        return new LanguagePair(getLanguage(sourceName), getLanguage(targetName));
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