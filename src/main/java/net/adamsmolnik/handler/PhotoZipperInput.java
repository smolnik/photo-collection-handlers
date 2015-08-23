package net.adamsmolnik.handler;

import java.util.List;

/**
 * @author asmolnik
 *
 */
public class PhotoZipperInput {

	private String zipBucket, zipKey;

	private List<PhotoKeyEntity> photoKeyEntities;

	public PhotoZipperInput(String zipBucket, String zipKey, List<PhotoKeyEntity> photoKeyEntities) {
		this.zipBucket = zipBucket;
		this.zipKey = zipKey;
		this.photoKeyEntities = photoKeyEntities;
	}

	public PhotoZipperInput() {

	}

	public String getZipBucket() {
		return zipBucket;
	}

	public void setZipBucket(String zipBucket) {
		this.zipBucket = zipBucket;
	}

	public String getZipKey() {
		return zipKey;
	}

	public void setZipKey(String zipKey) {
		this.zipKey = zipKey;
	}

	public List<PhotoKeyEntity> getPhotoKeyEntities() {
		return photoKeyEntities;
	}

	public void setPhotoKeyEntities(List<PhotoKeyEntity> photoKeyEntities) {
		this.photoKeyEntities = photoKeyEntities;
	}

	@Override
	public String toString() {
		return "PhotoZipperInput [zipBucket=" + zipBucket + ", zipKey=" + zipKey + ", photoKeyEntities=" + photoKeyEntities + "]";
	}

}
