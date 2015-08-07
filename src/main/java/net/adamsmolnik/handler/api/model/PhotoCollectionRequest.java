package net.adamsmolnik.handler.api.model;

/**
 * @author asmolnik
 *
 */
public class PhotoCollectionRequest extends PhotoRequest {

	public String photoTakenDate;

	public PhotoCollectionRequest() {

	}

	public PhotoCollectionRequest(String principalId) {
		super(principalId);
	}

}
