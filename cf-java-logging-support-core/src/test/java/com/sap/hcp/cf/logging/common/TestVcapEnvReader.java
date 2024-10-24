package com.sap.hcp.cf.logging.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class TestVcapEnvReader {

    @Test
    public void testWithEnv() {
        Map<String, String> tags = new HashMap<String, String>();
        VcapEnvReader.setEnvMap(EnvMap.getMap());
        VcapEnvReader.getAppInfos(tags, new HashSet<String>());
        assertTrue(tags.get(Fields.COMPONENT_NAME).equals(EnvMap.VCAP_APP_NAME));
        assertTrue(tags.get(Fields.COMPONENT_ID).equals(EnvMap.VCAP_APP_ID));
        assertTrue(tags.get(Fields.COMPONENT_INSTANCE).equals(EnvMap.VCAP_INSTANCE_IDX));
        assertTrue(tags.get(Fields.SPACE_ID).equals(EnvMap.VCAP_SPACE_ID));
        assertTrue(tags.get(Fields.SPACE_NAME).equals(EnvMap.VCAP_SPACE_NAME));
        assertTrue(tags.get(Fields.ORGANIZATION_ID).equals(EnvMap.VCAP_ORGANIZATION_ID));
        assertTrue(tags.get(Fields.ORGANIZATION_NAME).equals(EnvMap.VCAP_ORGANIZATION_NAME));
        VcapEnvReader.setEnvMap(null);
    }

    @Test
    public void testNoOverride() {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(Fields.COMPONENT_NAME, EnvMap.NOT_SET);
        VcapEnvReader.setEnvMap(EnvMap.getMap());
        VcapEnvReader.getAppInfos(tags, new HashSet<String>());
        assertTrue(tags.get(Fields.COMPONENT_NAME).equals(EnvMap.NOT_SET));
        assertTrue(tags.get(Fields.COMPONENT_ID).equals(EnvMap.VCAP_APP_ID));
        assertTrue(tags.get(Fields.COMPONENT_INSTANCE).equals(EnvMap.VCAP_INSTANCE_IDX));
        assertTrue(tags.get(Fields.SPACE_ID).equals(EnvMap.VCAP_SPACE_ID));
        assertTrue(tags.get(Fields.SPACE_NAME).equals(EnvMap.VCAP_SPACE_NAME));
        assertTrue(tags.get(Fields.ORGANIZATION_ID).equals(EnvMap.VCAP_ORGANIZATION_ID));
        assertTrue(tags.get(Fields.ORGANIZATION_NAME).equals(EnvMap.VCAP_ORGANIZATION_NAME));
        VcapEnvReader.setEnvMap(null);
    }
}
