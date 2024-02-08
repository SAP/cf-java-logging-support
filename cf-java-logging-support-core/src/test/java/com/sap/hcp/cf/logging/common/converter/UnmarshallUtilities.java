package com.sap.hcp.cf.logging.common.converter;

import com.fasterxml.jackson.jr.ob.JSON;

import java.util.List;
import java.util.Map;

public final class UnmarshallUtilities {

    private UnmarshallUtilities() {
    }

    public static Map<String, Object> unmarshal(CharSequence sb) throws Exception {
        String source = sb.toString().trim();
        String json = isEnclosedInBrackets(source) ? source : "{" + source + "}";
        return JSON.std.mapFrom(json);
    }

    private static boolean isEnclosedInBrackets(String source) {
        return source.startsWith("{") && source.endsWith("}");
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> unmarshalCustomFields(CharSequence sb, String prefix) throws Exception {
        Map<String, Object> prefixed = (Map<String, Object>) unmarshal(sb).get(prefix);
        return (List<Map<String, Object>>) prefixed.get("string");
    }

}
