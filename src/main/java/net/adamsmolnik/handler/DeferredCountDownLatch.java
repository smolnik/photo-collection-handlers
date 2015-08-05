package net.adamsmolnik.handler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.adamsmolnik.handler.exception.PhotoZipperHandlerException;

/**
 * @author asmolnik
 *
 */
public class DeferredCountDownLatch {

    private final AtomicInteger counter = new AtomicInteger();

    private final Object lock = new Object();

    private volatile CountDownLatch latch;

    public void await(int count) {
        latch = new CountDownLatch(count);
        new Thread() {
            public void run() {
                synchronized (lock) {
                    while (countDown()) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            // Deliberately ignored
                        }
                    }
                }
            }

            private boolean countDown() {
                int c = counter.get();
                for (int i = 0; i < c; i++) {
                    latch.countDown();
                }
                return count - c > 0;
            }

        }.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new PhotoZipperHandlerException(e);
        }
    }

    public void countDown() {
        synchronized (lock) {
            counter.incrementAndGet();
            if (latch != null) {
                lock.notify();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        DeferredCountDownLatch latch = new DeferredCountDownLatch();
        latch.countDown();
        latch.countDown();
        latch.countDown();
        latch.countDown();
        latch.countDown();

        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    TimeUnit.SECONDS.sleep(4);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        latch.await(5);
        System.out.println("ok");
    }

}
