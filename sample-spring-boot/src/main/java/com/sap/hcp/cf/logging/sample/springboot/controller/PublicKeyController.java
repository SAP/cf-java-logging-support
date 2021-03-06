package com.sap.hcp.cf.logging.sample.springboot.controller;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sap.hcp.cf.logging.sample.springboot.keystore.TokenKeyProvider;

/**
 * This controller provides an endpoint to get the public key used for signing
 * the tokens generated by the {@link TokenController}.
 */
@RestController
public class PublicKeyController {

	private TokenKeyProvider keyProvider;

	public PublicKeyController(@Autowired TokenKeyProvider keyProvider) {
		this.keyProvider = keyProvider;
	}

	/**
	 * Returns the public key in Base64 encoding used to sign the JWTs created by
	 * the {@link TokenController}.
	 * 
	 * @return
	 */
	@GetMapping("/publickey")
	public String getPublicKey() {
		String keyId = keyProvider.getPrivateKeyId();
		RSAPublicKey publicKey = keyProvider.getPublicKeyById(keyId);
		return Base64.getEncoder().encodeToString(publicKey.getEncoded());
	}

}
