package com.sap.hcp.cf.logging.common.customfields;

public class CustomField {
    private final String key;
    private final Object value;

    private CustomField(String key, Object value) {
        validateNotNull(key, "key must not be null");
        this.key = key;
        this.value = value;
    }

    /**
     * Include <i>custom field</i> key:value in the JSON output.
     *
     * @param key
     *         the key, must not be null
     * @param value
     *         the value, {@link String#valueOf} will be used to generate the String representation
     * @return a CustomField object representing key=value
     * @throws IllegalArgumentException
     *         if key is null
     */
    public static CustomField customField(String key, Object value) {
        return new CustomField(key, value);
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return key + "=" + String.valueOf(value);
    }

    private void validateNotNull(Object obj, String msg) throws IllegalArgumentException {
        if (obj == null) {
            if (msg != null) {
                throw new IllegalArgumentException(msg);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

}
