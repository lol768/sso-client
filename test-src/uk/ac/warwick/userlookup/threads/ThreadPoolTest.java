package uk.ac.warwick.userlookup.threads;

import junit.framework.TestCase;

public class ThreadPoolTest extends TestCase {
	private int inc = 0;
	
	private void increment() {
		inc++;
	}
	
	public void testThreadPool() {
		inc = 0;
		ThreadPool pool = new ThreadPool(5);
		for (int i=0; i<100; i++) {
			pool.assign(new Runnable() {
				public void run() {
					increment();
				}
			});
		}
		pool.complete();
		assertEquals(100, inc);
	}
}
