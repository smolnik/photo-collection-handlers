package net.adamsmolnik.handler;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DeferredBoundedLatchQueue<E> implements AutoCloseable {

	private final BlockingQueue<E> bq = new LinkedBlockingQueue<>();

	private final List<Thread> threads = new LinkedList<>();
	
	private final Object latch = new Object();

	private volatile int count = -1;

	public void put(E e) throws InterruptedException {
		bq.put(e);
	}

	public E take() throws InterruptedException {
		Thread t = Thread.currentThread();
		threads.add(t);
		E e = bq.take();
		if (count > -1 && --count == 0) {
			synchronized (latch) {
				latch.notify();
			}
		}
		return e;
	}

	public void waitFor(int count) throws InterruptedException {
		if (count < 1) {
			throw new IllegalArgumentException("Count argument cannot be less than 1");
		}
		synchronized (latch) {
			this.count = count;
			while(count != 0){
				latch.wait();
			}
		}
	}

	@Override
	public void close() {

	}

}
