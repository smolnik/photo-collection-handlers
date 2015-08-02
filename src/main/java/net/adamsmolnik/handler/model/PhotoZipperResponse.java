package net.adamsmolnik.handler.model;

/**
 * @author asmolnik
 *
 */
public class PhotoZipperResponse {

	public final int itemsCount;

	public final String dowloadUrl;

	public PhotoZipperResponse(int itemsCount, String dowloadUrl) {
		this.itemsCount = itemsCount;
		this.dowloadUrl = dowloadUrl;
	}

	@Override
	public String toString() {
		return "PhotosZipperResponse [itemsCount=" + itemsCount + ", dowloadUrl=" + dowloadUrl + "]";
	}

}
