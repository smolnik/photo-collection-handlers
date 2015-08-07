package net.adamsmolnik.handler.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author asmolnik
 *
 */
public class PhotoCollectionResponse {

	private final int hashCode;

	private final String date;

	private final List<PhotoItem> photoItems;

	public PhotoCollectionResponse(String date, List<PhotoItem> photoItems) {
		this.date = date;
		this.photoItems = photoItems;
		this.hashCode = hashCode();
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((photoItems == null) ? 0 : photoItems.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PhotoCollectionResponse other = (PhotoCollectionResponse) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (photoItems == null) {
			if (other.photoItems != null)
				return false;
		} else if (!photoItems.equals(other.photoItems))
			return false;
		return true;
	}

	public String getDate() {
		return date;
	}

	public int getHashCode() {
		return hashCode;
	}

	public List<PhotoItem> getPhotoItems() {
		return new ArrayList<>(photoItems);
	}
}
