package net.adamsmolnik.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * @author asmolnik
 *
 */
public abstract class PhotoHandler {

	protected static final ThreadLocal<AmazonS3> thS3 = new ThreadLocal<AmazonS3>() {

		@Override
		protected AmazonS3 initialValue() {
			return new AmazonS3Client();
		}

	};

	protected static final ThreadLocal<AmazonDynamoDB> thDb = new ThreadLocal<AmazonDynamoDB>() {

		@Override
		protected AmazonDynamoDB initialValue() {
			return new AmazonDynamoDBClient();
		}

	};

	protected String mapIdentity(String principalId) {
		// TODO
		return "default";
	}

	protected KeyAttribute newUserIdentityKeyAttribute(String principalId) {
		return new KeyAttribute("userId", mapIdentity(principalId));
	}

}
