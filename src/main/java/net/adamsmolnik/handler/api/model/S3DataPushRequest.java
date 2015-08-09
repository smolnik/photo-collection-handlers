package net.adamsmolnik.handler.api.model;

/**
 * @author asmolnik
 *
 */
public class S3DataPushRequest {

	public String bucket;

	public String zipKey;

	public String zipOutput;

	@Override
	public String toString() {
		return "S3DataPushRequest [bucket=" + bucket + ", zipKey=" + zipKey + ", zipOutput size=~" + (zipOutput == null ? 0 : zipOutput.length())
				+ "]";
	}

}
