package net.adamsmolnik.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author asmolnik
 *
 */
class ZipComposer implements AutoCloseable {

	private final ExecutorService composer = Executors.newSingleThreadExecutor();

	private final int numberOfEntries;

	private final CountDownLatch latch;

	ZipComposer(int numberOfEntries) {
		this.numberOfEntries = numberOfEntries;
		this.latch = new CountDownLatch(numberOfEntries);
	}

	Future<byte[]> compose(BlockingQueue<CachedPhoto> cpQueue) {
		return composer.submit(() -> {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (ZipOutputStream zos = new ZipOutputStream(baos)) {
				int counter = numberOfEntries;
				while (--counter >= 0) {
					CachedPhoto cp = cpQueue.take();
					try (InputStream is = cp.is) {
						doZip(cp.fileName, is, zos);
					} finally {
						latch.countDown();
					}
				}
			}
			return baos.toByteArray();
		});
	}

	private static void doZip(String fileName, InputStream is, ZipOutputStream zos) throws IOException {
		ZipEntry zipEntry = new ZipEntry(fileName);
		zos.putNextEntry(zipEntry);
		byte[] buf = new byte[8192];
		int bytesRead;
		while ((bytesRead = is.read(buf)) > 0) {
			zos.write(buf, 0, bytesRead);
		}
		zos.closeEntry();
	}

	public void await(long timeout, TimeUnit unit) throws InterruptedException {
		latch.await(timeout, unit);
	}

	@Override
	public void close() {
		composer.shutdownNow();
	}
}
