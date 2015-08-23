package net.adamsmolnik.handler.api.model;

/**
 * @author asmolnik
 *
 */
public class PhotoCollectionRequest extends PhotoRequest {

	private String photoTakenDate;

	public String getPhotoTakenDate() {
		return photoTakenDate;
	}

	public void setPhotoTakenDate(String photoTakenDate) {
		this.photoTakenDate = photoTakenDate;
	}

}
