package net.adamsmolnik.handler.api.model;

/**
 * @author asmolnik
 *
 */
public class PhotoZipperResponse {

	public final int itemsCount;

	public final long size;

	public final String zipKey, bucket, dowloadUrl;

	public PhotoZipperResponse(int itemsCount, long size, String zipKey, String bucket, String dowloadUrl) {
		this.itemsCount = itemsCount;
		this.size = size;
		this.zipKey = zipKey;
		this.bucket = bucket;
		this.dowloadUrl = dowloadUrl;
	}

	@Override
	public String toString() {
		return "PhotoZipperResponse [itemsCount=" + itemsCount + ", size=" + size + ", zipKey=" + zipKey + ", bucket=" + bucket + ", dowloadUrl="
				+ dowloadUrl + "]";
	}

}
