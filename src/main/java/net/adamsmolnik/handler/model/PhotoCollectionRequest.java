package net.adamsmolnik.handler.model;

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
