package net.adamsmolnik.handler;

/**
 * @author asmolnik
 *
 */
public class PhotoKeyEntity {

	private String bucket, key;

	public PhotoKeyEntity(String bucket, String key) {
		this.bucket = bucket;
		this.key = key;
	}

	public PhotoKeyEntity() {

	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return "PhotoKeyEntity [bucket=" + bucket + ", key=" + key + "]";
	}

}
