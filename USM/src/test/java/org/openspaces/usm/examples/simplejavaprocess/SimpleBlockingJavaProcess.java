/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.openspaces.usm.examples.simplejavaprocess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.openspaces.usm.examples.simplejavaprocess.SimpleBlockingJavaProcessMBean;
import org.openspaces.usm.examples.simplejavaprocess.SystemInReaderTask;

public class SimpleBlockingJavaProcess implements
		SimpleBlockingJavaProcessMBean {

	private AtomicInteger counter = new AtomicInteger();
	private final CountDownLatch latch = new CountDownLatch(1);
	private final ExecutorService executorService = Executors
			.newFixedThreadPool(2);

	public void startReaderTask() {
		executorService.submit(new SystemInReaderTask(latch));
	}

	public CountDownLatch getLatch() {
		return latch;
	}

	private void incCounter() {
		counter.incrementAndGet();
	}

	public void setCounter(final int counter) {
		this.counter.set(counter);
	}

	@Override
	public String getDetails() {
		return "DETAILS TEST";
	}

	@Override
	public String getType() {
		return SimpleBlockingJavaProcess.class.getName();
	}

	@Override
	public int getCounter() {
		return counter.get();
	}

	@Override
	public void die() {
		latch.countDown();
	}

	private void stopExecutorService() {
		executorService.shutdownNow();
	}

	private static Properties createProperties(final String[] args) {
		final Properties props = new Properties();
		String key = null;
		for (final String str : args) {
			if (key == null) {
				if (str.startsWith("-")) {
					key = str.substring(1);
				} else {
					key = str;
				}
			} else {
				props.setProperty(key, str);
				key = null;
			}
		}

		return props;

	}

	public static void main(final String[] args) throws Exception {

		// String fileOutputPath = System.getProperty("java.io.tmpdir") +
		// File.pathSeparatorChar + "simpleProcessLog.log";
		final SimpleBlockingJavaProcess simple = new SimpleBlockingJavaProcess();
		final Properties props = SimpleBlockingJavaProcess
				.createProperties(args);
		if (props.getProperty("port") != null) {
			String[] ports = props.getProperty("port").split(",");
			// Open multiple ports
			for (String port : ports) {
				simple.startServerTask(Integer.parseInt(port));
			}
		}

		String fileOutputPath = props.getProperty("filePath");
		final boolean dieOnParentDeath = Boolean.parseBoolean(props
				.getProperty("dieOnParentDeath", Boolean.TRUE.toString()));

		System.out.println("dieOnParentDeath = " + dieOnParentDeath);
		final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		final ObjectName mbeanName = new ObjectName(
				"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess");
		mbs.registerMBean(simple, mbeanName);
		simple.startReaderTask();
		boolean parentProcessDied = false;

		File f = null;
		if(fileOutputPath == null) {
			f = File.createTempFile("simpleProcessLog", ".log");
		} else {
			f = new File(fileOutputPath);
			if(!f.exists()) {
				f.createNewFile();
			}
		}
		System.out.println("Simple Java Process will write to file: "
				+ f.getAbsolutePath());

		// if(!f.exists()){
		// f.createNewFile();
		// }
		FileWriter fstream = new FileWriter(f);
		BufferedWriter out = new BufferedWriter(fstream);
		while (true) {
			simple.incCounter();
			if (simple.getCounter() != 3) {
				out.write(simple.getCounter());
			} else {
				out.write("Hello_World");
			}
			out.flush();
			try {
				System.out.println("system.out: Still alive...");
				System.err.println("system.err: Still alive...");
				if (!parentProcessDied) {
					parentProcessDied = simple.getLatch().await(5,
							TimeUnit.SECONDS);
					if (parentProcessDied) {
						System.out.println("Parent process Died");
						if (dieOnParentDeath) {
							System.out.println("Exiting");
							out.close();
							break;
						}
					}
				} else {
					Thread.sleep(1000);
				}

			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		out.close();
		simple.stopExecutorService();
		mbs.unregisterMBean(mbeanName);
		System.exit(0);

	}

	private void startServerTask(final int port) {
		this.executorService.submit(new Runnable() {

			@Override
			public void run() {

				try {
					System.out.println("Opening port: " + port);
					final ServerSocket ss = new ServerSocket(port);

					while (true) {
						final Socket sock = ss.accept();
						System.out.println("Got an incoming request");
						Thread.sleep(1000);
						sock.close();
					}

				} catch (final Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

	}

}
