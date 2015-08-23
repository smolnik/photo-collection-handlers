package net.adamsmolnik.handler.api.model;

/**
 * @author asmolnik
 *
 */
public class PhotoZipperInquiryRequest extends PhotoRequest {

	private String fromDate;

	private String toDate;

	public PhotoZipperInquiryRequest(String fromDate, String toDate) {
		this.fromDate = fromDate;
		this.toDate = toDate;
	}

	public PhotoZipperInquiryRequest() {

	}

	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}

	public String getToDate() {
		return toDate;
	}

	public void setToDate(String toDate) {
		this.toDate = toDate;
	}

}
