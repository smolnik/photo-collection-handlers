package net.adamsmolnik.handler.api.model;

import java.util.Arrays;

/**
 * @author asmolnik
 *
 */
public class PhotoItem {

	public final String bucket, photoKey, thumbnailKey;

	public final String[] metadata;

	public PhotoItem(String bucket, String photoKey, String thumbnailKey, String... metadata) {
		this.bucket = bucket;
		this.photoKey = photoKey;
		this.thumbnailKey = thumbnailKey;
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		return "PhotoItem [bucket=" + bucket + ", photoKey=" + photoKey + ", thumbnailKey=" + thumbnailKey + ", metadata=" + Arrays.toString(metadata)
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + Arrays.hashCode(metadata);
		result = prime * result + ((photoKey == null) ? 0 : photoKey.hashCode());
		result = prime * result + ((thumbnailKey == null) ? 0 : thumbnailKey.hashCode());
		return result;
	}

	public String getPhotoKey() {
		return photoKey;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PhotoItem other = (PhotoItem) obj;
		if (bucket == null) {
			if (other.bucket != null)
				return false;
		} else if (!bucket.equals(other.bucket))
			return false;
		if (!Arrays.equals(metadata, other.metadata))
			return false;
		if (photoKey == null) {
			if (other.photoKey != null)
				return false;
		} else if (!photoKey.equals(other.photoKey))
			return false;
		if (thumbnailKey == null) {
			if (other.thumbnailKey != null)
				return false;
		} else if (!thumbnailKey.equals(other.thumbnailKey))
			return false;
		return true;
	}

}
