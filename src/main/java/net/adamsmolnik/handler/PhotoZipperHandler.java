package net.adamsmolnik.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import net.adamsmolnik.handler.exception.PhotoZipperHandlerException;

/**
 * @author asmolnik
 *
 */
public class PhotoZipperHandler extends PhotoHandler {

	protected static final int MAX_CONNECTIONS = 60;

	public void handle(PhotoZipperInput pzi, Context context) {
		long then = System.currentTimeMillis();
		Logger log = new Logger(context);
		log.log("Request for zip of " + pzi);
		ClientConfiguration cc = new ClientConfiguration();
		cc.setMaxConnections(MAX_CONNECTIONS);
		AmazonS3 s3 = new AmazonS3Client(cc);
		ExecutorService es = Executors.newFixedThreadPool((int) (0.8 * MAX_CONNECTIONS) + 2);
		List<PhotoKeyEntity> pkEntities = pzi.getPhotoKeyEntities();
		int size = pkEntities.size();
		try (ZipComposer zc = new ZipComposer(size)) {
			BlockingQueue<CachedPhoto> cachedPhotosToBeZippedQueue = new LinkedBlockingQueue<>(size);
			Future<byte[]> zcFuture = zc.compose(cachedPhotosToBeZippedQueue);
			List<Future<Void>> itemFutures = new ArrayList<>();
			pkEntities.forEach(pkEntity -> {
				itemFutures.add(es.submit(() -> {
					String pk = pkEntity.getKey();
					String[] photoKeyParts = pk.split("/");
					String fileName = photoKeyParts[photoKeyParts.length - 1];
					S3Object s3Object = s3.getObject(pkEntity.getBucket(), pk);
					int length = (int) s3Object.getObjectMetadata().getContentLength();
					cachedPhotosToBeZippedQueue.put(new CachedPhoto(fileName, new ByteArrayInputStream(readBytes(length, s3Object))));
					return null;
				}));
			});
			for (Future<?> itemFuture : itemFutures) {
				itemFuture.get();
			}
			zc.await(5, TimeUnit.MINUTES);
			log.log(then, "Zipping finished");
			byte[] zipOutput = zcFuture.get();
			TransferManager tm = new TransferManager();
			try {
				log.log(then, "Transfer to s3 is about to start");
				ObjectMetadata om = new ObjectMetadata();
				om.setContentLength(zipOutput.length);
				Upload upload = tm.upload(pzi.getZipBucket(), pzi.getZipKey(), new ByteArrayInputStream(zipOutput), om);
				upload.waitForCompletion();
			} finally {
				tm.shutdownNow();
				log.log(then, getClass().getSimpleName() + " is about to complete");
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new PhotoZipperHandlerException(e);
		} finally {
			es.shutdownNow();
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

}
