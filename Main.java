import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    private static final ReentrantLock lockA = new ReentrantLock();
    private static final ReentrantLock lockB = new ReentrantLock();

    private static final int CPUs = Runtime.getRuntime().availableProcessors();
    private static final CountDownLatch latch = new CountDownLatch(CPUs);

    public static void main(String[] args) throws Exception {
        lockA.lock();

        // VT 1
        Thread.startVirtualThread(() -> {
                lockB.lock();
                lockA.lock();
                lockA.unlock();
                lockB.unlock();
        });
        Thread.sleep(1000);

        System.out.println("Starting pinned virtual threads");

        // VT 2..CPUs+1
        for (int i = 0; i < CPUs; i++) {
            Thread.startVirtualThread(() -> {
                synchronized (Main.class) {
                    lockB.lock();
                    lockB.unlock();
                    latch.countDown();
                    System.out.println("Exiting synchronized block");
                }
            });
        }

        // This should unblock VT1, which unblocks VT2..CPUs+1
        // Instead we deadlock, since VT1 can't execute as there are no
        // available platform threads.
        lockA.unlock();
        latch.await();
    }
}
