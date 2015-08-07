package net.adamsmolnik.handler;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author asmolnik
 *
 */
public class DeferredBoundedLatchQueue<E> implements AutoCloseable {

	private final BlockingQueue<E> bq = new LinkedBlockingQueue<>();

	private final Collection<Thread> threadsOnWait = new ConcurrentLinkedQueue<>();

	private final Object latchGuard = new Object();

	private int count = 0;

	private volatile boolean closed = false;

	public void put(E e) {
		try {
			bq.put(e);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}

	public Optional<E> take() {
		Thread t = Thread.currentThread();
		synchronized (latchGuard) {
			if (closed) {
				throw new IllegalStateException("Already closed");
			}
			threadsOnWait.add(t);
		}
		boolean externallyInterrupted = false;
		try {
			E e = bq.take();
			synchronized (latchGuard) {
				if (--count == 0) {
					latchGuard.notify();
				}
			}
			return Optional.of(e);
		} catch (InterruptedException e) {
			if (!closed) {
				t.interrupt();
				externallyInterrupted = true;
				throw new IllegalStateException(e);
			}
			return Optional.empty();
		} finally {
			synchronized (latchGuard) {
				threadsOnWait.remove(t);
				if (!externallyInterrupted) {
					Thread.interrupted();
				}
			}
		}
	}

	public void waitFor(int latchCount) throws InterruptedException {
		if (latchCount < 0) {
			throw new IllegalArgumentException("Latch count argument cannot be less than 0");
		}
		synchronized (latchGuard) {
			count = count + latchCount;
			while (count > 0) {
				latchGuard.wait();
			}
		}
		close();
	}

	@Override
	public final void close() {
		synchronized (latchGuard) {
			closed = true;
			threadsOnWait.forEach(Thread::interrupt);
		}
	}

	public boolean isClosed() {
		return closed;
	}

	public static void main(String[] args) throws Exception {
		DeferredBoundedLatchQueue<String> queue = new DeferredBoundedLatchQueue<>();
		for (int i = 0; i < 3; i++) {
			new Thread(() -> queue.take()).start();
			new Thread(() -> queue.take()).start();
			new Thread(() -> queue.take()).start();
		}

		for (int i = 0; i < 3; i++) {
			new Thread(() -> queue.put("")).start();
			new Thread(() -> queue.put("")).start();
			new Thread(() -> queue.put("")).start();
		}

		new Thread(() -> {
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (Exception e) {
				e.printStackTrace();
			}
			queue.put("");
		}).start();
		new Thread(() -> queue.take()).start();

		queue.waitFor(10);
		queue.close();
		System.out.println("ok");
	}

}