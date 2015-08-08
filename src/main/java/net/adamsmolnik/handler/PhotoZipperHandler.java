package net.adamsmolnik.handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;

import net.adamsmolnik.handler.api.model.PhotoZipperRequest;
import net.adamsmolnik.handler.api.model.PhotoZipperResponse;
import net.adamsmolnik.handler.exception.PhotoZipperHandlerException;

/**
 * Questionable usage of AWS Lambda service - seems to work, but keep in mind
 * the execution time limit of AWS API Gateway (up to 10 secs) and AWS Lambda
 * itself (up to 60 secs). Therefore, the response is sent back as soon as there
 * is enough data available to build meaningful content, prior to the handler
 * method actually complete.
 * 
 * 
 * @author asmolnik
 *
 */
public class PhotoZipperHandler extends PhotoHandler {

	private static final String ZIP_BUCKET = "zip.smolnik.photos";

	public PhotoZipperResponse handle(PhotoZipperRequest request, Context context) {
		Logger log = new Logger(context);
		Date then = new Date();
		log.log("Request for zipping from " + request.fromDate + " to " + request.toDate + " received ");
		String fromDate = request.fromDate;
		String toDate = request.toDate;
		String principalId = request.principalId;
		ItemCollection<QueryOutcome> items = fetchItemsFromDb(principalId, fromDate, toDate);
		if (!((Iterator<?>) items.iterator()).hasNext()) {
			return new PhotoZipperResponse(0, 0, "", "", "");
		}

		ExecutorService es = Executors.newFixedThreadPool(50);
		AmazonS3 s3 = thlS3.get();
		try (ZipComposer zc = new ZipComposer()) {
			AtomicInteger count = new AtomicInteger(0);
			Path tempPath = Files.createTempFile(null, null);
			File tempFile = tempPath.toFile();
			tempFile.deleteOnExit();
			DeferredBoundedLatchQueue<CachedPhoto> cpQueue = new DeferredBoundedLatchQueue<>();
			Future<?> zcFuture = zc.compose(tempPath, cpQueue);
			List<Future<?>> itemFutures = new ArrayList<>();
			items.forEach(item -> {
				count.incrementAndGet();
				itemFutures.add(es.submit(() -> {
					String imageKey = item.getString("photoKey");
					String[] imKeyParts = imageKey.split("/");
					String fileName = imKeyParts[imKeyParts.length - 1];
					S3Object s3Object = s3.getObject(item.getString("bucket"), imageKey);
					int size = (int) s3Object.getObjectMetadata().getContentLength();
					cpQueue.put(new CachedPhoto(fileName, new ByteArrayInputStream(readBytes(size, s3Object))));
				}));
			});
			for (Future<?> itemFuture : itemFutures) {
				itemFuture.get();
			}
			cpQueue.waitFor(count.get());
			zcFuture.get();
			String zipKey = mapIdentity(principalId) + "_" + fromDate.replaceAll("/", "") + "_" + toDate.replaceAll("/", "") + ".zip";
			log.log("Zipping finished after " + (new Date().getTime() - then.getTime()));
			es.submit(() -> {
				s3.putObject(ZIP_BUCKET, zipKey, tempFile);
				es.shutdown();
				log.log(getClass().getSimpleName() + " is about to complete after " + (new Date().getTime() - then.getTime()));
			});
			return new PhotoZipperResponse(count.get(), tempFile.length(), ZIP_BUCKET, zipKey,
					s3.generatePresignedUrl(ZIP_BUCKET, zipKey, new Date(Instant.now().toEpochMilli() + (24L * 3600 * 1000))).toString());
		} catch (IOException | InterruptedException | ExecutionException e) {
			throw new PhotoZipperHandlerException(e);
		}
	}

	private byte[] readBytes(int size, S3Object s3Object) {
		try (InputStream is = s3Object.getObjectContent()) {
			int pos = 0, length;
			byte[] bytesToRead = new byte[size];
			while ((length = is.read(bytesToRead, pos, bytesToRead.length)) > 0) {
				pos += length;
			}
			return bytesToRead;
		} catch (IOException e) {
			throw new PhotoZipperHandlerException(e);
		}
	}

	private ItemCollection<QueryOutcome> fetchItemsFromDb(String principalId, String fromDate, String toDate) {
		DynamoDB db = new DynamoDB(thlDb.get());
		Index index = db.getTable("photos").getIndex("photoTakenDate-index");
		ItemCollection<QueryOutcome> items = index.query(newUserIdentityKeyAttribute(principalId),
				new RangeKeyCondition("photoTakenDate").between(fromDate, toDate));
		return items;
	}
}
