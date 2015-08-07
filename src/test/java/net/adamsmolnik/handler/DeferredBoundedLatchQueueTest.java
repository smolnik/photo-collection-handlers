package net.adamsmolnik.handler;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author asmolnik
 *
 */
public class DeferredBoundedLatchQueueTest {

	@Test
	public void shouldEndSeamlesslyTest() throws Exception {
		int threadNumber = 3;
		DeferredBoundedLatchQueue<String> queue = new DeferredBoundedLatchQueue<>();
		for (int i = 0; i < threadNumber; i++) {
			new Thread(() -> queue.take()).start();
		}

		for (int i = 0; i < threadNumber; i++) {
			new Thread(() -> queue.put("")).start();
		}

		new Thread(() -> {
			try {
				TimeUnit.SECONDS.sleep(1);
				queue.put("");
			} catch (Exception e) {
				// deliberately ignored
			}
		}).start();
		new Thread(() -> queue.take()).start();

		queue.waitFor(4);
		queue.close();
		Assert.assertTrue(queue.isClosed());
	}

}
