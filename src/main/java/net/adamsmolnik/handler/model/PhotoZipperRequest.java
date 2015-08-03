package net.adamsmolnik.handler.model;

/**
 * @author asmolnik
 *
 */
public class PhotoZipperRequest extends PhotoRequest {

	public String fromDate;

	public String toDate;

	public PhotoZipperRequest() {
		
	}

	public PhotoZipperRequest(String principalId, String fromDate, String toDate) {
		super(principalId);
		this.fromDate = fromDate;
		this.toDate = toDate;
	}

}
