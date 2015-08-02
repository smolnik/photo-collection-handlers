package net.adamsmolnik.handler;

import com.amazonaws.services.dynamodbv2.document.KeyAttribute;

/**
 * @author asmolnik
 *
 */
public abstract class PhotoHandler {

	protected String mapIdentity(String principalId) {
		// TODO
		return "default";
	}

	protected KeyAttribute newUserIdentityKeyAttribute(String principalId) {
		return new KeyAttribute("userId", mapIdentity(principalId));
	}

}
