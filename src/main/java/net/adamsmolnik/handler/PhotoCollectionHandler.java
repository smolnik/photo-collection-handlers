package net.adamsmolnik.handler;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;

import net.adamsmolnik.handler.model.PhotoCollectionRequest;
import net.adamsmolnik.handler.model.PhotoCollectionResponse;
import net.adamsmolnik.handler.model.PhotoItem;

/**
 * @author asmolnik
 *
 */
public class PhotoCollectionHandler extends PhotoHandler {

	public PhotoCollectionResponse getPhotoCollection(PhotoCollectionRequest request) {
		String ptDate = request.photoTakenDate;
		DynamoDB db = new DynamoDB(thDb.get());
		Index index = db.getTable("photos").getIndex("photoTakenDate-index");
		ItemCollection<QueryOutcome> items = index.query(newUserIdentityKeyAttribute(request.principalId),
				new RangeKeyCondition("photoTakenDate").eq(ptDate));

		return new PhotoCollectionResponse(ptDate,
				StreamSupport.stream(items.spliterator(), false)
						.map(item -> new PhotoItem(item.getString("bucket"), item.getString("photoKey"), item.getString("thumbnailKey"),
								item.getString("photoTakenDate") + " " + item.getString("photoTakenTime"),
								item.getString("madeBy") + " " + item.getString("model")))
						.collect(Collectors.toList()));
	}

}
