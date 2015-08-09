package net.adamsmolnik.handler;

import java.lang.management.ManagementFactory;
import java.util.Date;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * @author asmolnik
 *
 */
public abstract class PhotoHandler {

	protected static class Logger {

		private final String processId = ManagementFactory.getRuntimeMXBean().getName();

		private final Context context;

		protected Logger(Context context) {
			this.context = context;
		}

		protected void log(String message) {
			doLog("[" + getBaseMessage(message) + "]");
		}

		protected void log(Date then, String message) {
			doLog("[duration since then: " + (new Date().getTime() - then.getTime()) + ", " + getBaseMessage(message) + "]");
		}

		private String getBaseMessage(String message) {
			return "processId: " + processId + ", thread: " + Thread.currentThread() + ", " + context.getAwsRequestId() + ", date: " + new Date()
					+ ", message: " + message;
		}

		private void doLog(String message) {
			context.getLogger().log(message);
		}

	}

	protected static final int MAX_CONNECTIONS = 60;

	protected static final ThreadLocal<AmazonS3> thlS3 = new ThreadLocal<AmazonS3>() {

		@Override
		protected AmazonS3 initialValue() {
			ClientConfiguration cc = new ClientConfiguration();
			cc.setMaxConnections(MAX_CONNECTIONS);
			return new AmazonS3Client(cc);
		}

	};

	protected static final ThreadLocal<AmazonDynamoDB> thlDb = new ThreadLocal<AmazonDynamoDB>() {

		@Override
		protected AmazonDynamoDB initialValue() {
			return new AmazonDynamoDBClient();
		}

	};

	protected static final ThreadLocal<AWSLambda> thlLbd = new ThreadLocal<AWSLambda>() {

		@Override
		protected AWSLambda initialValue() {
			return new AWSLambdaClient();
		}

	};

	protected String mapIdentity(String principalId) {
		// TODO
		return "default";
	}

	protected KeyAttribute newUserIdentityKeyAttribute(String principalId) {
		return new KeyAttribute("userId", mapIdentity(principalId));
	}

	protected void cleanup() {
		thlS3.set(new AmazonS3Client());
		thlDb.set(new AmazonDynamoDBClient());
	}

}
