package net.adamsmolnik.handler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Region;

import net.adamsmolnik.handler.exception.PhotoZipperHandlerException;
import net.adamsmolnik.handler.model.PhotoZipperRequest;
import net.adamsmolnik.handler.model.PhotoZipperResponse;

/**
 * @author asmolnik
 *
 */
public class PhotoZipperHandler extends PhotoHandler {

	private static final String ZIP_BUCKET = "zip.smolnik.photos";

	private final AmazonDynamoDB clientDb = new AmazonDynamoDBClient();

	private final AmazonS3 s3 = new AmazonS3Client();

	public PhotoZipperResponse handle(PhotoZipperRequest request) {
		String fromDate = request.fromDate;
		String toDate = request.toDate;
		DynamoDB db = new DynamoDB(clientDb);
		Index index = db.getTable("photos").getIndex("photoTakenDate-index");
		ItemCollection<QueryOutcome> items = index.query(newUserIdentityKeyAttribute(request.principalId),
				new RangeKeyCondition("photoTakenDate").between(fromDate, toDate));
		if (!((Iterator<?>) items.iterator()).hasNext()) {
			return new PhotoZipperResponse(0, "");
		}
		try {
			AtomicInteger count = new AtomicInteger();
			Path tempPath = Files.createTempFile(null, null);
			File tempFile = tempPath.toFile();
			tempFile.deleteOnExit();
			try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempPath))) {
				items.forEach(item -> {
					count.incrementAndGet();
					String imageKey = item.getString("photoKey");
					String[] imKeyParts = imageKey.split("/");
					String fileName = imKeyParts[imKeyParts.length - 1];
					try (InputStream is = s3.getObject(item.getString("bucket"), imageKey).getObjectContent()) {
						doZip(fileName, is, zos);
					} catch (IOException e) {
						throw new PhotoZipperHandlerException(e);
					}
				});
			}
			String zipKey = mapIdentity(request.principalId) + "_" + fromDate.replaceAll("/", "") + "_" + toDate.replaceAll("/", "") + ".zip";
			s3.setRegion(Region.EU_Frankfurt.toAWSRegion());
			s3.putObject(ZIP_BUCKET, zipKey, tempFile);
			return new PhotoZipperResponse(count.get(),
					s3.generatePresignedUrl(ZIP_BUCKET, zipKey, new Date(Instant.now().toEpochMilli() + (24L * 3600 * 1000))).toString());
		} catch (Exception e) {
			throw new PhotoZipperHandlerException(e);
		}
	}

	public static void doZip(String fileName, InputStream is, ZipOutputStream zos) throws IOException {
		ZipEntry zipEntry = new ZipEntry(fileName);
		zos.putNextEntry(zipEntry);
		byte[] buf = new byte[8192];
		int bytesRead;
		while ((bytesRead = is.read(buf)) > 0) {
			zos.write(buf, 0, bytesRead);
		}
		zos.closeEntry();
	}

	public static void main(String[] args) {
		PhotoZipperHandler pzh = new PhotoZipperHandler();
		System.out.println(pzh.handle(new PhotoZipperRequest(null, "2015-06-12", "2015-06-12")));
	}

}
