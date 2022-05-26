package com.eugene.wc.protocol.identity;

import static com.eugene.wc.protocol.api.util.StringUtils.toUtf8;

import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.KeyPair;
import com.eugene.wc.protocol.api.crypto.PrivateKey;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityFactory;
import com.eugene.wc.protocol.api.identity.IdentityId;
import com.eugene.wc.protocol.api.identity.LocalIdentity;

import javax.inject.Inject;

public class IdentityFactoryImpl implements IdentityFactory {

	private final CryptoComponent crypto;

	@Inject
	public IdentityFactoryImpl(CryptoComponent crypto) {
		this.crypto = crypto;
	}

	@Override
	public Identity createIdentity(String name, PublicKey publicKey) {
		IdentityId id = generateId(name, publicKey);

		return new Identity(id, publicKey, name);
	}

	@Override
	public LocalIdentity createLocalIdentity(String name) {
		KeyPair keyPair = crypto.generateSignatureKeyPair();
		PublicKey publicKey = keyPair.getPublicKey();
		PrivateKey privateKey = keyPair.getPrivateKey();

		IdentityId id = generateId(name, publicKey);
		return new LocalIdentity(id, publicKey, name, privateKey);
	}

	private IdentityId generateId(String name, PublicKey publicKey) {
		return new IdentityId(crypto.hash(IdentityId.LABEL, toUtf8(name), publicKey.getBytes()));
	}
}
