package net.adamsmolnik.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.util.json.JSONObject;

import net.adamsmolnik.handler.api.model.PhotoZipperInquiryRequest;
import net.adamsmolnik.handler.api.model.PhotoZipperInquiryResponse;

/**
 * @author asmolnik
 *
 */
public class PhotoZipperInquiryHandler extends PhotoHandler {

	private static final String ZIP_BUCKET = "zip.smolnik.photos";

	public PhotoZipperInquiryResponse handle(PhotoZipperInquiryRequest request, Context context) {
		long then = System.currentTimeMillis();
		Logger log = new Logger(context);
		String fromDate = request.getFromDate();
		String toDate = request.getToDate();
		String principalId = request.getPrincipalId();
		log.log("Request for zipping from " + fromDate + " to " + toDate + " received");
		ItemCollection<QueryOutcome> items = fetchItemsFromDb(principalId, fromDate, toDate);
		if (!((Iterator<?>) items.iterator()).hasNext()) {
			return new PhotoZipperInquiryResponse(0, "", "", "");
		}
		AtomicInteger count = new AtomicInteger();
		List<PhotoKeyEntity> pkEntities = new ArrayList<>();
		items.forEach(item -> {
			count.incrementAndGet();
			pkEntities.add(new PhotoKeyEntity(item.getString("bucket"), item.getString("photoKey")));
		});
		log.log(then, "Response from Db with " + count + " items received");
		String zipKey = mapIdentity(principalId) + "_" + fromDate.replaceAll("/", "") + "_" + toDate.replaceAll("/", "") + ".zip";
		String fn = "photo-zipper-handler";
		String payload = new JSONObject(new PhotoZipperInput(ZIP_BUCKET, zipKey, pkEntities)).toString();
		new AWSLambdaClient().invoke(new InvokeRequest().withFunctionName(fn).withInvocationType(InvocationType.Event).withPayload(payload));
		log.log(then, "Lambda function " + fn + " invoked with payload " + payload);
		return new PhotoZipperInquiryResponse(count.get(), ZIP_BUCKET, zipKey, new AmazonS3Client()
				.generatePresignedUrl(ZIP_BUCKET, zipKey, new Date(Instant.now().toEpochMilli() + (24L * 3600 * 1000))).toString());
	}

	private ItemCollection<QueryOutcome> fetchItemsFromDb(String principalId, String fromDate, String toDate) {
		DynamoDB db = new DynamoDB(new AmazonDynamoDBClient());
		RangeKeyCondition condition = fromDate.equals(toDate) ? new RangeKeyCondition("photoTakenDate").eq(fromDate)
				: new RangeKeyCondition("photoTakenDate").between(fromDate, toDate);
		Index index = db.getTable("photos").getIndex("photoTakenDate-index");
		return index.query(newUserIdentityKeyAttribute(principalId), condition);
	}

	public static void main(String[] args) {
		new PhotoZipperInquiryHandler().handle(new PhotoZipperInquiryRequest("2015-06-01", "2015-08-30"), new Context() {

			@Override
			public int getRemainingTimeInMillis() {
				return 0;
			}

			@Override
			public int getMemoryLimitInMB() {
				return 0;
			}

			@Override
			public LambdaLogger getLogger() {
				return new LambdaLogger() {

					@Override
					public void log(String string) {
						System.out.println(string);
					}
				};
			}

			@Override
			public String getLogStreamName() {
				return null;
			}

			@Override
			public String getLogGroupName() {
				return null;
			}

			@Override
			public CognitoIdentity getIdentity() {
				return null;
			}

			@Override
			public String getFunctionName() {
				return null;
			}

			@Override
			public ClientContext getClientContext() {
				return null;
			}

			@Override
			public String getAwsRequestId() {
				return null;
			}
		});
	}
}
