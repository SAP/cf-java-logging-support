package com.sap.hcp.cf.logging.common;

import com.fasterxml.jackson.jr.ob.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to read CloudFoundry environment variable(s) and pull relevant fields from those.
 * <p>
 * Right now, the things we look at are
 * <ul>
 * <li><code>ENV_VCAP_APPLICATION</code></li>
 * <li><code>ENV_CF_INSTANCE_IP</code></li>
 * <li><code>ENV_CF_LANDSCAPE_ID</code></li>
 * </ul>
 */
public class VcapEnvReader {

    public static final String ENV_VCAP_APPLICATION = "VCAP_APPLICATION";
    public static final String ENV_CF_INSTANCE_IP = "CF_INSTANCE_IP";
    public static final String ENV_LANSCAPE_ID = "LANDSCAPE_ID";
    private static final Logger LOG = LoggerFactory.getLogger(VcapEnvReader.class);
    /*
     * -- field names within VCAP_APPLICATION env JSON object
     */
    private static final String CF_APPLICATION_ID = "application_id";
    private static final String CF_APPLICATION_NAME = "application_name";
    private static final String CF_INSTANCE_INDEX = "instance_index";
    private static final String CF_SPACE_ID = "space_id";
    private static final String CF_SPACE_NAME = "space_name";
    private static final String CF_ORGANIZATION_ID = "organization_id";
    private static final String CF_ORGANIZATION_NAME = "organization_name";

    /*
     * for testing purposes only!
     */
    private static Map<String, String> ENV_MAP;

    /**
     * Retrieves Cloud Foundry related settings from environment variables, currently <code>VCAP_APPLICATION</code>,
     * <code>LANDSCAPE_ID</code> and
     * <code>CF_INSTANCE_IP</code>
     *
     * @return the map containing the retrieved key/value pairs
     */
    public static Map<String, String> getEnvMap() {
        Map<String, String> result = new HashMap<String, String>();
        getAppInfos(result, null);
        return result;
    }

    /*
     * FOR UNIT TESTING PURPOSES ONLY
     */
    protected static void setEnvMap(Map<String, String> envMap) {
        ENV_MAP = envMap;
    }

    /**
     * Retrieves Cloud Foundry environment variables and fills tag map accordingly. Also returns the tag keys that have
     * been found in those variables.
     * <p>
     * <b>Will not override values that are already stored in the tags map!</b>
     *
     * @param tags
     *         the map instance into which the key/value pairs will be put
     * @param envKeys
     *         the set of keys that have been retrieved from the variables.
     */
    public static void getAppInfos(Map<String, String> tags, Set<String> envKeys) {
        String vcap = getEnv(ENV_VCAP_APPLICATION);
        if (vcap != null) {
            try {
                Map<String, Object> envMap = JSON.std.mapFrom(vcap);
                /*
                 * -- all the fields we're interested in are at the top-level.
                 * -- if there's ever anything nested, we need to change this
                 * accordingly!
                 */
                addField(tags, envKeys, envMap, CF_APPLICATION_NAME, Fields.COMPONENT_NAME);
                addField(tags, envKeys, envMap, CF_APPLICATION_ID, Fields.COMPONENT_ID);
                addField(tags, envKeys, envMap, CF_INSTANCE_INDEX, Fields.COMPONENT_INSTANCE);
                addField(tags, envKeys, envMap, CF_SPACE_ID, Fields.SPACE_ID);
                addField(tags, envKeys, envMap, CF_SPACE_NAME, Fields.SPACE_NAME);
                addField(tags, envKeys, envMap, CF_ORGANIZATION_ID, Fields.ORGANIZATION_ID);
                addField(tags, envKeys, envMap, CF_ORGANIZATION_NAME, Fields.ORGANIZATION_NAME);
            } catch (Exception ex) {
                LOG.error("Cannot get infos from environment", ex);
                return;
            }
        }

        String cfInstanceIp = getEnv(ENV_CF_INSTANCE_IP);
        if (cfInstanceIp != null) {
            tags.put(Fields.CONTAINER_ID, cfInstanceIp);
        }
    }

    private static String getEnv(String name) {
        return ENV_MAP != null ? ENV_MAP.get(name) : System.getenv(name);
    }

    private static void addField(Map<String, String> tags, Set<String> tagKeys, Map<String, Object> envMap,
                                 String envKey, String tagKey) {
        if (!tags.containsKey(tagKey)) {
            Object tn = envMap.get(envKey);
            if (tn != null) {
                if (tagKeys != null) {
                    tagKeys.add(tagKey);
                }
                tags.put(tagKey, tn.toString());
            }
        }
    }
}
