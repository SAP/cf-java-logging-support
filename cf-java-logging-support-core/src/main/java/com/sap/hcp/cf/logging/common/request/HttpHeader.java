package com.sap.hcp.cf.logging.common.request;

import java.util.List;

public interface HttpHeader {

    boolean isPropagated();

    String getName();

    String getField();

    List<HttpHeader> getAliases();

    String getFieldValue();
}
