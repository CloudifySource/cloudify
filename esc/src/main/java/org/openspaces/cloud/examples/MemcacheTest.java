package org.openspaces.cloud.examples;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

/***********
 * Memcache Test.
 * @author barakme
 *
 */
public final class MemcacheTest {

	private MemcacheTest() {
		// private constructor to avoid instantiation
	}
	
	/*********
	 * The test main method.
	 * @param args .
	 */
	public static void main(final String[] args) {
		try {

			final MemcachedClient memClient = new XMemcachedClient("localhost",
					11211);

			// System.out.println("Connected to memcache "
			// +memClient.getAvailableServers());
			// testSimpleMemcached(memClient);
			MemcacheTest.testMemcachedPerformance(memClient);
			memClient.shutdown();

		} catch (final Exception ex) {

			ex.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

	/* CHECKSTYLE:OFF */
	private static void testMemcachedPerformance(final MemcachedClient memClient)
			throws InterruptedException, ExecutionException, TimeoutException,
			MemcachedException {

		long startTime = System.currentTimeMillis();
		final int iterations = 1000;
		for (int i = 0; i < iterations; i++) {
			memClient.set(Integer.toString(i), 3600 * 60, new Data(i, "message"
					+ i, null));
		}

		long endTime = System.currentTimeMillis();
		double optime = (double) (endTime - startTime) / iterations;
		System.out.println("Total memClient.set time: " + (endTime - startTime)
				+ "ms " + "optime=" + optime + " " + (long) (1000 / optime)
				+ "ops/sec");

		startTime = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			memClient.get(Integer.toString(i));
		}

		endTime = System.currentTimeMillis();
		optime = (double) (endTime - startTime) / iterations;
		System.out.println("Total memClient.get time: " + (endTime - startTime)
				+ "ms " + "optime=" + optime + " " + (long) (1000 / optime)
				+ "ops/sec");
	}
	/* CHECKSTYLE:ON */

}
