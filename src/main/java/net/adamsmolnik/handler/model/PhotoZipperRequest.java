package net.adamsmolnik.handler.model;

/**
 * @author asmolnik
 *
 */
public class PhotoZipperRequest extends PhotoRequest {

	public final String fromDate;

	public final String toDate;

	public PhotoZipperRequest(String principalId, String fromDate, String toDate) {
		super(principalId);
		this.fromDate = fromDate;
		this.toDate = toDate;
	}

}
