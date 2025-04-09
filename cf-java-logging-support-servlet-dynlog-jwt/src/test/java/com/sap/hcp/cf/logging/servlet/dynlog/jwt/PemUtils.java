package com.sap.hcp.cf.logging.servlet.dynlog.jwt;

import java.security.PublicKey;
import java.util.Base64;

class PemUtils {

    private PemUtils() {
    }

    static String createPublicKeyPem(PublicKey key) {
        String encoded = Base64.getEncoder().encodeToString(key.getEncoded());
        StringBuilder sb = new StringBuilder("-----BEGIN PUBLIC KEY-----\n");
        for (int i = 0; i < encoded.length() / 64 + 1; i++) {
            sb.append(encoded, i * 64, Math.min(encoded.length(), (i + 1) * 64)).append("\n");
        }
        return sb.append("-----END PUBLIC KEY-----").toString();
    }
}
