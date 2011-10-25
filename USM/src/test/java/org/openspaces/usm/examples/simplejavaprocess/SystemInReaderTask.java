/**
 * 
 */
package org.openspaces.usm.examples.simplejavaprocess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

/**
 * @author rafi
 * @since 8.0.3
 */
public class SystemInReaderTask implements Runnable{

	private final CountDownLatch latch;

	/**
	 * 
	 */
	public SystemInReaderTask(CountDownLatch latch) {
		this.latch = latch;
	}
	
	public void run() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.println("reading line...");
			String readLine = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			latch.countDown();
		}
	}

}
