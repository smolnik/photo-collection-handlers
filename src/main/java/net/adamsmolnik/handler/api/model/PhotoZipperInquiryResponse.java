package net.adamsmolnik.handler.api.model;

/**
 * @author asmolnik
 *
 */
public class PhotoZipperInquiryResponse {

	private int itemsCount;

	private String zipKey, bucket, dowloadUrl;

	public PhotoZipperInquiryResponse(int itemsCount, String zipKey, String bucket, String dowloadUrl) {
		this.itemsCount = itemsCount;
		this.zipKey = zipKey;
		this.bucket = bucket;
		this.dowloadUrl = dowloadUrl;
	}

	public int getItemsCount() {
		return itemsCount;
	}

	public String getZipKey() {
		return zipKey;
	}

	public String getBucket() {
		return bucket;
	}

	public String getDowloadUrl() {
		return dowloadUrl;
	}

	@Override
	public String toString() {
		return "PhotoZipperResponse [itemsCount=" + itemsCount + ", zipKey=" + zipKey + ", bucket=" + bucket + ", dowloadUrl=" + dowloadUrl + "]";
	}

}
