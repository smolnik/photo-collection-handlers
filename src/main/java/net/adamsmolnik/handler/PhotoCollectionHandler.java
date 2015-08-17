package net.adamsmolnik.handler;

import java.util.Comparator;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.lambda.runtime.Context;

import net.adamsmolnik.handler.api.model.PhotoCollectionRequest;
import net.adamsmolnik.handler.api.model.PhotoCollectionResponse;
import net.adamsmolnik.handler.api.model.PhotoItem;

/**
 * @author asmolnik
 *
 */
public class PhotoCollectionHandler extends PhotoHandler {

	public PhotoCollectionResponse handle(PhotoCollectionRequest request, Context context) {
		Date then = new Date();
		Logger log = new Logger(context);
		log.log("Request for " + request.photoTakenDate + " received");
		String ptDate = request.photoTakenDate;
		DynamoDB db = new DynamoDB(thlDb.get());
		Index index = db.getTable("photos").getIndex("photoTakenDate-index");
		ItemCollection<QueryOutcome> items = index.query(newUserIdentityKeyAttribute(request.principalId),
				new RangeKeyCondition("photoTakenDate").eq(ptDate));
		log.log(then, "QueryOutcome received for " + request.photoTakenDate);
		PhotoCollectionResponse response = new PhotoCollectionResponse(ptDate,
				StreamSupport.stream(items.spliterator(), false)
						.map(item -> new PhotoItem(item.getString("bucket"), item.getString("photoKey"), item.getString("thumbnailKey"),
								item.getString("photoTakenDate") + " " + item.getString("photoTakenTime"),
								item.getString("madeBy") + " " + item.getString("model")))
						.sorted(Comparator.comparing(PhotoItem::getPhotoKey)).collect(Collectors.toList()));
		log.log(then, getClass().getSimpleName() + " is about to complete");
		return response;
	}

}
