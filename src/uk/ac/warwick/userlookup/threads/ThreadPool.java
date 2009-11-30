package uk.ac.warwick.userlookup.threads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Java Thread Pool
 * 
 * This is a thread pool that for Java, it is simple to use and gets the job
 * done. This program and all supporting files are distributed under the Limited
 * GNU Public License (LGPL, http://www.gnu.org).
 * 
 * This is the main class for the thread pool. You should create an instance of
 * this class and assign tasks to it.
 * 
 * For more information visit http://www.jeffheaton.com.
 * 
 * @author Jeff Heaton (http://www.jeffheaton.com)
 * @version 1.0
 */
public class ThreadPool<T extends Runnable> {
	/**
	 * The threads in the pool.
	 */
	protected Thread threads[] = null;
	/**
	 * The backlog of assignments, which are waiting for the thread pool.
	 */
	Collection<T> assignments = new ArrayList<T>(3);
	/**
	 * A Done object that is used to track when the thread pool is done, that is
	 * has no more work to perform.
	 */
	protected Done done = new Done();

	/**
	 * The constructor.
	 * 
	 * @param size
	 *            How many threads in the thread pool.
	 */
	public ThreadPool(int size) {
		if (size < 1) {
			throw new IllegalArgumentException("Needs at least one thread");
		}
		threads = new WorkerThread[size];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new WorkerThread<T>(this);
			threads[i].start();
		}
	}

	/**
	 * Add a task to the thread pool. Any class which implements the Runnable
	 * interface may be assigned. When this task runs, its run method will be
	 * called.
	 * 
	 * @param r
	 *            An object that implements the Runnable interface
	 */
	public synchronized void assign(T r) {
		done.workerBegin();
		assignments.add(r);
		notify();
	}

	/**
	 * Get a new work assignment.
	 * 
	 * @return A new assignment
	 */
	public synchronized T getAssignment() {
		try {
			while (!assignments.iterator().hasNext()) {
				wait();
			}
			T r = assignments.iterator().next();
			assignments.remove(r);
			return r;
		} catch (InterruptedException e) {
			done.workerEnd();
			return null;
		}
	}
	
	/**
	 * Returns a new list of the currently waiting assignments
	 * that have not yet been started.
	 */
	public synchronized List<T> getWaitingAssignments() {
		return new ArrayList<T>(assignments);
	}

	/**
	 * Called to block the current thread until the thread pool has no more
	 * work.
	 */
	public void complete() {
		done.waitBegin();
		done.waitDone();
	}

	protected void finalize() {
		done.reset();
		for (int i = 0; i < threads.length; i++) {
			threads[i].interrupt();
			done.workerBegin();
		}
		done.waitDone();
	}
	
	public int running() {
		return done.size();
	}
}

/**
 * The worker threads that make up the thread pool.
 * 
 * @author Jeff Heaton
 * @version 1.0
 */
class WorkerThread<T extends Runnable> extends Thread {
	/**
	 * True if this thread is currently processing.
	 */
	public boolean busy;
	/**
	 * The thread pool that this object belongs to.
	 */
	public ThreadPool<T> owner;

	/**
	 * The constructor.
	 * 
	 * @param o
	 *            the thread pool
	 */
	WorkerThread(ThreadPool<T> o) {
		owner = o;
		setDaemon(true);
	}

	/**
	 * Scan for and execute tasks.
	 */
	public void run() {
		Runnable target = null;

		do {
			target = owner.getAssignment();
			if (target != null) {
				try {
					target.run();
				} finally {
					owner.done.workerEnd();
				}
			}
		} while (target != null);
	}
}
