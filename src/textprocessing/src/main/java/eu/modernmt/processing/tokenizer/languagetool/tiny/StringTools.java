package eu.modernmt.processing.tokenizer.languagetool.tiny;

class StringTools {
    /**
     * Helper method to replace calls to {@code "".equals()}.
     *
     * @param str String to check
     * @return true if string is empty or {@code null}
     */
    public static boolean isEmpty(final String str) {
        return str == null || str.length() == 0;
    }

    /**
     * Checks if a string contains only whitespace, including all Unicode
     * whitespace, but not the non-breaking space. This differs a bit from the
     * definition of whitespace in Java 7 because of the way we want to interpret Khmer.
     *
     * @param str String to check
     * @return true if the string is whitespace-only
     */
    public static boolean isWhitespace(final String str) {
        if ("\u0002".equals(str) // unbreakable field, e.g. a footnote number in OOo
                || "\u0001".equals(str)) { // breakable field in OOo
            return false;
        }
        final String trimStr = str.trim();
        if (isEmpty(trimStr)) {
            return true;
        }
        if (trimStr.length() == 1) {
            if ("\u200B".equals(str)) {
                // We need u200B​​ to be detected as whitespace for Khmer, as it was the case before Java 7.
                return true;
            }
            return Character.isWhitespace(trimStr.charAt(0));
        }
        return false;
    }
}
