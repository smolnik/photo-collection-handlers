package net.adamsmolnik.handler;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.handlers.TracingHandler;

import net.adamsmolnik.handler.api.model.PhotoCollectionRequest;
import net.adamsmolnik.handler.api.model.PhotoCollectionResponse;
import net.adamsmolnik.handler.api.model.PhotoItem;

/**
 * @author asmolnik
 *
 */
public class PhotoCollectionHandler extends PhotoHandler {

	private final AmazonDynamoDB dbClient = AmazonDynamoDBClientBuilder.standard()
			.withRequestHandlers(new TracingHandler(AWSXRay.getGlobalRecorder())).build();

	public PhotoCollectionResponse handle(PhotoCollectionRequest request, Context context) {
		long then = System.currentTimeMillis();
		validateRequest(request);
		Logger log = new Logger(context);
		String ptDate = request.getPhotoTakenDate();
		String principalId = request.getPrincipalId();
		log.log("Request for " + ptDate + " received");

		DynamoDB db = new DynamoDB(dbClient);
		Index index = db.getTable("photos").getIndex("photoTakenDate-index");
		ItemCollection<QueryOutcome> items = index.query(newUserIdentityKeyAttribute(principalId),
				new RangeKeyCondition("photoTakenDate").eq(ptDate));
		log.log(then, "QueryOutcome received for " + ptDate);
		PhotoCollectionResponse response = new PhotoCollectionResponse(ptDate,
				StreamSupport.stream(items.spliterator(), false)
						.map(item -> new PhotoItem(item.getString("bucket"), item.getString("photoKey"),
								item.getString("thumbnailKey"),
								item.getString("photoTakenDate") + " " + item.getString("photoTakenTime"),
								item.getString("srcPhotoName"), getOptionalItems(item, "madeBy", "model")))
						.sorted(Comparator.comparing(PhotoItem::getPhotoKey)).collect(Collectors.toList()));
		log.log(then, getClass().getSimpleName() + " is about to complete");
		return response;
	}

	private void validateRequest(PhotoCollectionRequest request) {
		String ptDate = request.getPhotoTakenDate();
		if (ptDate == null || ptDate.trim().isEmpty()) {
			throw new IllegalArgumentException("photoTakenDate parameter cannot be null or empty");
		}
	}

	private String getOptionalItems(Item item, String... attrNames) {
		StringBuilder sb = new StringBuilder();
		Arrays.asList(attrNames).stream().filter(attrName -> item.get(attrName) != null)
				.forEach(attrName -> sb.append(item.get(attrName)).append(" "));
		return sb.length() < 1 ? "" : sb.substring(0, sb.length() - 1);
	}

}
