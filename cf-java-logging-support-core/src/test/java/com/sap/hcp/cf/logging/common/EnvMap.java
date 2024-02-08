package com.sap.hcp.cf.logging.common;

import java.util.HashMap;
import java.util.Map;

public class EnvMap {
    public static final String VCAP_APP_NAME = "test-app";
    public static final String VCAP_APP_ID = "867b99ba-1caa-4d91-b2ee-82982234f58a";
    public static final String VCAP_SPACE_NAME = "test-space";
    public static final String VCAP_SPACE_ID = "69ca5b0f-cc66-4f8e-9d17-f102f60142e2";
    public static final String VCAP_ORGANIZATION_NAME = "test-org";
    public static final String VCAP_ORGANIZATION_ID = "6b8cf2f9-bf9a-45f6-8774-0a0f42cb27f3";
    public static final String VCAP_INSTANCE_IDX = "0";
    public static final String VCAP_APPLICATION =
            "{" + "\"application_id\":\"" + VCAP_APP_ID + "\", " + "\"application_name\":\"" + VCAP_APP_NAME + "\", " + "\"application_uris\":[  " + "\"testapp.testdomain.com\" " + "], " + "\"application_version\":\"92720bd3-2735-489d-bf4b-e5bb05c752e7\", " + "\"cf_api\":\"https://api.cf.sap.hana.ondemand.com\", " + "\"host\":\"0.0.0.0\", " + "\"instance_id\":\"34b12c70-5443-426f-75c4-3260\", " + "\"instance_index\":\"" + VCAP_INSTANCE_IDX + "\", " + "\"limits\":{  " + "\"disk\":1024, " + "\"fds\":16384, " + "\"mem\":1024 " + "}, " + "\"name\":\"d048888-logging-sample-app\", " + "\"organization_id\":\"" + VCAP_ORGANIZATION_ID + "\", " + "\"organization_name\":\"" + VCAP_ORGANIZATION_NAME + "\", " + "\"port\":8080, " + "\"process_id\":\"a6026141-8d69-4e9d-b62d-8e1d982f7953\", " + "\"process_type\":\"web\", " + "\"space_id\":\"" + VCAP_SPACE_ID + "\", " + "\"space_name\":\"" + VCAP_SPACE_NAME + "\", " + "\"uris\":[  " + "\"testapp.testdomain.com\" " + "], " + "\"version\":\"92720bd3-2735-489d-bf4b-e5bb05c752e7\" " + "}";

    public static final String NOT_SET = "not-set";

    public static Map<String, String> getMap() {
        Map<String, String> envMap = new HashMap<String, String>();
        envMap.put(VcapEnvReader.ENV_VCAP_APPLICATION, VCAP_APPLICATION);
        return envMap;
    }
}
