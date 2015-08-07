package net.adamsmolnik.handler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author asmolnik
 *
 */
class ZipComposer implements AutoCloseable {

	private final ExecutorService composer = Executors.newSingleThreadExecutor();

	Future<?> compose(Path zipOutputPath, DeferredBoundedLatchQueue<CachedPhoto> cpQueue) {
		return composer.submit(() -> {
			try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipOutputPath))) {
				while (!cpQueue.isClosed()) {
					Optional<CachedPhoto> cpOptional = cpQueue.take();
					if (!cpOptional.isPresent()) {
						continue;
					}
					CachedPhoto cp = cpOptional.get();
					try (InputStream is = cp.is) {
						doZip(cp.fileName, is, zos);
					}
				}
			}
			return null;
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

	@Override
	public void close() {
		composer.shutdownNow();
	}
}
