package net.adamsmolnik.handler;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Date;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import net.adamsmolnik.handler.api.model.S3DataPushRequest;
import net.adamsmolnik.handler.api.model.S3DataPushResponse;

/**
 * @author asmolnik
 *
 */
public class S3DataPushHandler extends PhotoHandler {

	public S3DataPushResponse handle(S3DataPushRequest request, Context context) {
		Date then = new Date();
		Logger log = new Logger(context);
		log.log("Received: " + request);
		byte[] zipOutput = Base64.getDecoder().decode(request.zipOutput);
		log.log(then, "Decoded");
		TransferManager tm = null;
		try {
			tm = new TransferManager();
			ObjectMetadata om = new ObjectMetadata();
			om.setContentLength(zipOutput.length);
			Upload upload = tm.upload(request.bucket, request.zipKey, new ByteArrayInputStream(zipOutput), om);
			upload.waitForCompletion();
		} catch (Exception e) {
			cleanup();
			log.log(then, "Exception occured: " + e.getLocalizedMessage());
		} finally {
			if (tm != null) {
				tm.shutdownNow();
			}
			log.log(then, getClass().getSimpleName() + " is about to complete");
		}
		return new S3DataPushResponse("OK");

	}

}
