import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;

interface VoltageWorker extends Runnable{

    public void shutdown();
    public void execute(VoltageWork a);


}
class WorkerThread implements VoltageWorker {
    boolean shouldExit;
    Object lock;
    VoltageWork w;

    WorkerThread() {
        shouldExit = false;
        lock = new Object();
        this.run();
    }

    public void execute(VoltageWork in) {
       synchronized(lock) {
           w = in;
           lock.notify();
       }
    }

    public void shutdown() {

        shouldExit = true;
        synchronized (lock) {
            lock.notify();
        }
    }


    private boolean __wait() {
        try {
            synchronized(lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            return false;
        }
        if(shouldExit)
            return false;
        else
            return true;
    }

    public void run() {
        /**
         * This will always keep on waiting until someone calls workDone
         * This will always keep on waiting until someone calls doWork. Then it
         * will do work and wait again
         * A SIMPLE WORKER THREAD USING PRIMITVES*/

        while (!shouldExit) {
            try {
                if (__wait()) {
                    w.work();
                }
            } catch (VoltageException e) {
                /**
                 Handle the exception
                 * */
            }

        }

    }
}

class ThreadPool{
    PriorityBlockingQueue<VoltageWorker> threads;
    int numThreads;
    public ThreadPool(){
        __init(5);
    }
    public ThreadPool(int num){
        __init(num);
    }
    private void __init(int n){
        threads = new PriorityBlockingQueue<>();
        numThreads = n;
        for(int i = 0 ; i < numThreads ; i++){
            threads.add(new WorkerThread());
        }
    }
    public void execute(VoltageWork a) {
        boolean workDone = false;
        VoltageWorker t = null;
        while (!workDone) {
            try {
                t = threads.take();
                t.execute(a);
                workDone = true;
                threads.put(t);
            } catch (InterruptedException e) {
                if (workDone) {
                    if (t != null) {
                        threads.put(t);
                    }
                    break;
                }
            }
        }
    }
    public void shutdownPool(){
        int count = 0;
        VoltageWorker t;
        while(count < numThreads){
            try {
                t = threads.take();
                t.shutdown();
                count++;
            }catch(InterruptedException e){
                if(count >= numThreads) break;
            }
        }
    }
}


class VoltageException extends Exception{
    VoltageException(String message, Throwable cause){
        super(message,cause);
    }
}
interface VoltageWork{
    public void work() throws VoltageException;
}
/***
 * This is all the user has to implement
 * ***/
class Work implements VoltageWork{
    private void doREST(){
        System.out.println("Did a REST call ");
    }

    public void work() throws VoltageException{
        doREST();
    }
}

public class TheMainThread {
    public static void main(String [] args){
        ThreadPool t = new ThreadPool();
        ArrayList<VoltageWork> work = new ArrayList<>();
        for(int i = 0 ; i < 100000 ; i ++){
            work.add(new Work());
        }
        for(int i = 0 ; i < 100000 ; i ++){
            t.execute(work.get(i));
        }
        t.shutdownPool();

    }
}