package net.adamsmolnik.handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.adamsmolnik.handler.exception.PhotoZipperHandlerException;
import net.adamsmolnik.handler.model.PhotoZipperRequest;
import net.adamsmolnik.handler.model.PhotoZipperResponse;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;

/**
 * @author asmolnik
 *
 */
public class PhotoZipperHandler extends PhotoHandler {

    private static class CachedPhoto {

        private String fileName;

        private InputStream is;

        private CachedPhoto(String fileName, InputStream is) {
            this.fileName = fileName;
            this.is = is;
        }

    }

    private static final String ZIP_BUCKET = "zip.smolnik.photos";

    public PhotoZipperResponse handle(PhotoZipperRequest request) {
        String fromDate = request.fromDate;
        String toDate = request.toDate;
        String principalId = request.principalId;
        ItemCollection<QueryOutcome> items = fetchItemsFromDb(principalId, fromDate, toDate);
        if (!((Iterator<?>) items.iterator()).hasNext()) {
            return new PhotoZipperResponse(0, "");
        }

        BlockingQueue<CachedPhoto> cpQueue = new LinkedBlockingQueue<>();
        ExecutorService es = Executors.newFixedThreadPool(50);
        DeferredCountDownLatch latch = new DeferredCountDownLatch();
        AmazonS3 s3 = thS3.get();
        try {
            AtomicInteger count = new AtomicInteger();
            Path tempPath = Files.createTempFile(null, null);
            File tempFile = tempPath.toFile();
            tempFile.deleteOnExit();
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempPath))) {
                Thread composer = new Thread(() -> composeZip(cpQueue, zos, latch));
                composer.start();
                items.forEach(item -> {
                    es.submit(() -> {
                        count.incrementAndGet();
                        String imageKey = item.getString("photoKey");
                        String[] imKeyParts = imageKey.split("/");
                        String fileName = imKeyParts[imKeyParts.length - 1];
                        S3Object s3Object = s3.getObject(item.getString("bucket"), imageKey);
                        try (InputStream is = s3Object.getObjectContent()) {
                            int len, cursor;
                            byte[] bytesToRead = new byte[(int) s3Object.getObjectMetadata().getContentLength()];
                            do {
                                len = is.read(bytesToRead, 0, bytesToRead.length);
                            } while (len < bytesToRead.length);
                            cpQueue.add(new CachedPhoto(fileName, new ByteArrayInputStream(bytesToRead)));
                        } catch (IOException e) {
                            throw new PhotoZipperHandlerException(e);
                        }
                    });
                });
                latch.await(count.get());
                composer.interrupt();
            }
            String zipKey = mapIdentity(principalId) + "_" + fromDate.replaceAll("/", "") + "_" + toDate.replaceAll("/", "") + ".zip";
            s3.setRegion(Region.EU_Frankfurt.toAWSRegion());
            s3.putObject(ZIP_BUCKET, zipKey, tempFile);
            return new PhotoZipperResponse(count.get(), s3.generatePresignedUrl(ZIP_BUCKET, zipKey, new Date(Instant.now().toEpochMilli() + (24L * 3600 * 1000))).toString());
        } catch (IOException e) {
            throw new PhotoZipperHandlerException(e);
        } finally {
            s3.setRegion(Region.US_Standard.toAWSRegion());
            es.shutdownNow();
        }
    }

    private ItemCollection<QueryOutcome> fetchItemsFromDb(String principalId, String fromDate, String toDate) {
        DynamoDB db = new DynamoDB(thDb.get());
        Index index = db.getTable("photos").getIndex("photoTakenDate-index");
        ItemCollection<QueryOutcome> items = index.query(newUserIdentityKeyAttribute(principalId), new RangeKeyCondition("photoTakenDate").between(fromDate, toDate));
        return items;
    }

    public static void composeZip(BlockingQueue<CachedPhoto> cpQueue, ZipOutputStream zos, DeferredCountDownLatch latch) {
        try {
            while (true) {
                CachedPhoto cp = cpQueue.take();
                try (InputStream is = cp.is) {
                    doZip(cp.fileName, is, zos);
                } catch (IOException e) {
                    throw new PhotoZipperHandlerException(e);
                } finally {
                    latch.countDown();
                    System.out.println("dd");
                }
            }
        } catch (InterruptedException e2) {
            // Let the enclosing thread to move forward
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
        Date then = new Date();
        PhotoZipperHandler pzh = new PhotoZipperHandler();
        System.out.println(pzh.handle(new PhotoZipperRequest(null, "2015-06-12", "2015-08-12")));
        System.out.println(new Date().getTime() - then.getTime());
    }

}
