package com.eugene.wc.protocol.api.identity;

import com.eugene.wc.protocol.api.crypto.PublicKey;

public interface IdentityFactory {

	Identity createIdentity(String name, PublicKey publicKey);

	LocalIdentity createLocalIdentity(String name);
}
