package com.sap.hcp.cf.logging.common;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestVcapEnvReader {

    @Test
    public void testWithEnv() {
        Map<String, String> tags = new HashMap<String, String>();
        VcapEnvReader.setEnvMap(EnvMap.getMap());
        VcapEnvReader.getAppInfos(tags, new HashSet<String>());
        assertThat(tags).extracting(Fields.COMPONENT_NAME).isEqualTo(EnvMap.VCAP_APP_NAME);
        assertThat(tags).extracting(Fields.COMPONENT_ID).isEqualTo(EnvMap.VCAP_APP_ID);
        assertThat(tags).extracting(Fields.COMPONENT_INSTANCE).isEqualTo(EnvMap.VCAP_INSTANCE_IDX);
        assertThat(tags).extracting(Fields.SPACE_ID).isEqualTo(EnvMap.VCAP_SPACE_ID);
        assertThat(tags).extracting(Fields.SPACE_NAME).isEqualTo(EnvMap.VCAP_SPACE_NAME);
        assertThat(tags).extracting(Fields.ORGANIZATION_ID).isEqualTo(EnvMap.VCAP_ORGANIZATION_ID);
        assertThat(tags).extracting(Fields.ORGANIZATION_NAME).isEqualTo(EnvMap.VCAP_ORGANIZATION_NAME);
        VcapEnvReader.setEnvMap(null);
    }

    @Test
    public void testNoOverride() {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(Fields.COMPONENT_NAME, EnvMap.NOT_SET);
        VcapEnvReader.setEnvMap(EnvMap.getMap());
        VcapEnvReader.getAppInfos(tags, new HashSet<String>());
        assertThat(tags).extracting(Fields.COMPONENT_NAME).isEqualTo(EnvMap.NOT_SET);
        assertThat(tags).extracting(Fields.COMPONENT_ID).isEqualTo(EnvMap.VCAP_APP_ID);
        assertThat(tags).extracting(Fields.COMPONENT_INSTANCE).isEqualTo(EnvMap.VCAP_INSTANCE_IDX);
        assertThat(tags).extracting(Fields.SPACE_ID).isEqualTo(EnvMap.VCAP_SPACE_ID);
        assertThat(tags).extracting(Fields.SPACE_NAME).isEqualTo(EnvMap.VCAP_SPACE_NAME);
        assertThat(tags).extracting(Fields.ORGANIZATION_ID).isEqualTo(EnvMap.VCAP_ORGANIZATION_ID);
        assertThat(tags).extracting(Fields.ORGANIZATION_NAME).isEqualTo(EnvMap.VCAP_ORGANIZATION_NAME);
        VcapEnvReader.setEnvMap(null);
    }
}
