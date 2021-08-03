package jrp.utils.concurrency.lock;

public interface PriorityLock {

    void lock(int priority);
    void unlock();
    void lockMaximumPriority();

}
