package net.adamsmolnik.handler;

import java.lang.management.ManagementFactory;
import java.util.Date;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * @author asmolnik
 *
 */
public abstract class PhotoHandler {

	protected class Logger {

		private final String processId = ManagementFactory.getRuntimeMXBean().getName();

		private final Context context;

		protected Logger(Context context) {
			this.context = context;
		}

		protected void log(String message) {
			context.getLogger().log("[processId: " + processId + ", thread: " + Thread.currentThread() + ", " + context.getAwsRequestId() + ", date: "
					+ new Date() + ", message: " + message + "]");
		}

	}

	protected static final ThreadLocal<AmazonS3> thlS3 = new ThreadLocal<AmazonS3>() {

		@Override
		protected AmazonS3 initialValue() {
			return new AmazonS3Client();
		}

	};

	protected static final ThreadLocal<AmazonDynamoDB> thlDb = new ThreadLocal<AmazonDynamoDB>() {

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
