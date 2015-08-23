package net.adamsmolnik.handler;

import java.lang.management.ManagementFactory;
import java.util.Date;

import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.lambda.runtime.Context;

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

		protected void log(long then, String message) {
			doLog("[duration since then: " + (System.currentTimeMillis() - then) + ", " + getBaseMessage(message) + "]");
		}

		private String getBaseMessage(String message) {
			return "processId: " + processId + ", thread: " + Thread.currentThread() + ", " + context.getAwsRequestId() + ", date: " + new Date()
					+ ", message: " + message;
		}

		private void doLog(String message) {
			context.getLogger().log(message);
		}

	}

	protected String mapIdentity(String principalId) {
		// TODO
		return "default";
	}

	protected KeyAttribute newUserIdentityKeyAttribute(String principalId) {
		return new KeyAttribute("userId", mapIdentity(principalId));
	}

}
