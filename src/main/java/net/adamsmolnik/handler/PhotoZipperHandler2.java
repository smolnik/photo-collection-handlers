package net.adamsmolnik.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
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
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;

import net.adamsmolnik.handler.api.model.PhotoZipperRequest;
import net.adamsmolnik.handler.api.model.PhotoZipperResponse;
import net.adamsmolnik.handler.exception.PhotoZipperHandlerException;

/**
 * DOES NOT WORK PROPERLY due to event param size constraint
 * "... byte payload is too large for the Event invocation type (limit 131072 bytes)"
 * !
 * 
 * Experimental and questionable usage of AWS Lambda service - seems to work in
 * general, as long as Lambda instances with the function handled there are up
 * and running (not in a dormant or passive state), but still keep in mind the
 * execution time limit of AWS API Gateway (up to 10 secs) and AWS Lambda itself
 * (up to 60 secs). Therefore, the response is sent back as soon as there is
 * enough data available to build meaningful content (i.e. presigned url of zip
 * output file along its size and number of entries), prior to the handler
 * method actually complete the transfer of the outcome zip file to S3.
 * 
 * 
 * @author asmolnik
 *
 */
public class PhotoZipperHandler2 extends PhotoHandler {

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

        AmazonS3 s3 = thlS3.get();
        ExecutorService es = Executors.newFixedThreadPool((int) (0.8 * MAX_CONNECTIONS) + 2);
        try (ZipComposer zc = new ZipComposer()) {
            AtomicInteger count = new AtomicInteger();
            DeferredBoundedLatchQueue<CachedPhoto> cachedPhotosToBeZippedQueue = new DeferredBoundedLatchQueue<>();
            Future<byte[]> zcFuture = zc.compose(cachedPhotosToBeZippedQueue);
            List<Future<?>> itemFutures = new ArrayList<>();
            items.forEach(item -> {
                count.incrementAndGet();
                itemFutures.add(es.submit(() -> {
                    String imageKey = item.getString("photoKey");
                    String[] imKeyParts = imageKey.split("/");
                    String fileName = imKeyParts[imKeyParts.length - 1];
                    S3Object s3Object = s3.getObject(item.getString("bucket"), imageKey);
                    int size = (int) s3Object.getObjectMetadata().getContentLength();
                    cachedPhotosToBeZippedQueue.put(new CachedPhoto(fileName, new ByteArrayInputStream(readBytes(size, s3Object))));
                }));
            });
            for (Future<?> itemFuture : itemFutures) {
                itemFuture.get();
            }
            cachedPhotosToBeZippedQueue.waitFor(count.get());
            String zipKey = mapIdentity(principalId) + "_" + fromDate.replaceAll("/", "") + "_" + toDate.replaceAll("/", "") + ".zip";
            log.log(then, "Zipping finished");
            AWSLambda lbd = thlLbd.get();
            byte[] zipOutput = zcFuture.get();
            InvokeRequest invokeRequest = new InvokeRequest().withInvocationType(InvocationType.Event).withPayload(createJsonRequest(zipKey, zipOutput)).withFunctionName("s3-data-push-handler");
            lbd.invoke(invokeRequest);
            return new PhotoZipperResponse(count.get(), zipOutput.length, ZIP_BUCKET, zipKey, s3.generatePresignedUrl(ZIP_BUCKET, zipKey, new Date(Instant.now().toEpochMilli() + (24L * 3600 * 1000)))
                    .toString());
        } catch (InterruptedException | ExecutionException e) {
            cleanup();
            throw new PhotoZipperHandlerException(e);
        } finally {
            es.shutdownNow();
        }
    }

    private static String createJsonRequest(String zipKey, byte[] zipOutput) {
        return "{\"bucket\":\"" + ZIP_BUCKET + "\",\"zipKey\":\"" + zipKey + "\",\"zipOutput\":\"" + Base64.getEncoder().encodeToString(zipOutput) + "\"}";
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
        RangeKeyCondition condition = fromDate.equals(toDate) ? new RangeKeyCondition("photoTakenDate").eq(fromDate) : new RangeKeyCondition("photoTakenDate").between(fromDate, toDate);
        Index index = db.getTable("photos").getIndex("photoTakenDate-index");
        return index.query(newUserIdentityKeyAttribute(principalId), condition);
    }
}
