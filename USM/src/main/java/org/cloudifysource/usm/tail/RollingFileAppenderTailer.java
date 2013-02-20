/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.usm.tail;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * tail a RollingFileAppender logs folder without interfering with the RFA rolling action. in-order to avoid locking the
 * file and by that preventing the RFA from rolling the file, this tailer will sample all files periodically without
 * opening the files and when finding that a file has been modified, only then open the file, "grab" the newly added
 * lines and close the file when done.
 *
 * @author adaml
 *
 */
public class RollingFileAppenderTailer implements Runnable {

	/*********
	 * Handler interface which allows a client to delegate the handling of new lines found by the tailer to a custom
	 * class.
	 *
	 * @author barakme
	 *
	 */
	public interface LineHandler {

		/****************
		 * Called when a new line is found.
		 *
		 * @param fileName
		 *            .
		 * @param line
		 *            .
		 */
		void handleLine(final String fileName, final String line);
	}

	/******
	 * Logger implementation of the line handler interface.
	 * @author barakme
	 *
	 */
	private static class DefaultLineHandler implements LineHandler {

		@Override
		public void handleLine(final String fileName, final String line) {
			logger.info(line);

		}

	}

	private static final String BREAK_BY_LINES_NO_EMPTY_LINES_REGEX = "\\r?\\n+";
	private static final int DEFAULT_SAMPLING_DELAY = 2000;
	private final String logsDirectory;
	private final String regex;
	private final Pattern lineSplitPattern = Pattern.compile(BREAK_BY_LINES_NO_EMPTY_LINES_REGEX);

	private final Map<String, RollingFileReader> logFileMap = new HashMap<String, RollingFileReader>();

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(RollingFileAppenderTailer.class.getName());

	private LineHandler handler = new DefaultLineHandler();

	/**
	 * Create a new RollingFileAppenderTailer given the file-name regex and the directory where the log files will be
	 * located.
	 *
	 * @param dir
	 *            - the path to the directory of the log files to be tailed.
	 * @param regex
	 *            - regular expression for file names to be tailed.
	 */
	public RollingFileAppenderTailer(final String dir, final String regex) {
		this.logsDirectory = dir;
		this.regex = regex;
	}

	/****************
	 * Creates a new Tailer with a callback that handles each new line.
	 *
	 * @param dir
	 *            - the path to the directory of the log files to be tailed.
	 * @param regex
	 *            - regular expression for file names to be tailed.
	 * @param handler
	 *            - the callback that handles new lines.
	 */
	public RollingFileAppenderTailer(final String dir, final String regex, final LineHandler handler) {
		this.logsDirectory = dir;
		this.regex = regex;
		this.handler = handler;
	}

	/**
	 * Create a new RollingFileAppenderTailer given the file-name regex, the directory where the log files will be saved
	 * and the time period between sampling the files.
	 *
	 * @param dir
	 *            - the path to the directory of the log files to be tailed.
	 * @param regex
	 *            - regular expression for file names to be tailed.
	 * @param samplingDelay
	 *            - the time delay between sampling of files.
	 */
	public RollingFileAppenderTailer(final String dir, final String regex, final long samplingDelay) {
		this.logsDirectory = dir;
		this.regex = regex;
	}

	/**
	 * Start a new tailer on a predetermined folder.
	 *
	 * @param directory
	 *            - logs directory to be tailed.
	 * @param regex
	 *            - expected log file name format.
	 */
	public static void start(final String directory, final String regex) {
		ScheduledExecutorService executor;
		executor = Executors.newScheduledThreadPool(1);
		executor.scheduleWithFixedDelay(new RollingFileAppenderTailer(directory, regex), 0, DEFAULT_SAMPLING_DELAY,
				TimeUnit.MILLISECONDS);

	}

	/**
	 * Start a new tailer on a predetermined folder.
	 *
	 * @param directory
	 *            - logs directory to be tailed.
	 * @param regex
	 *            - expected log file name format.
	 * @param samplingDelay .
	 */
	public static void start(final String directory, final String regex, final long samplingDelay) {
		ScheduledExecutorService executor;
		executor = Executors.newScheduledThreadPool(1);
		executor.scheduleWithFixedDelay(new RollingFileAppenderTailer(directory, regex), 0, samplingDelay,
				TimeUnit.MILLISECONDS);
	}

	/*****************
	 * The synchronized statement is used to make sure that only one invocation of the tailer will execute at any one
	 * time. Within the context of the USM, the tailer runs in an async task every 5 seconds, but may be called on a
	 * separate thread as well. For instance, if the USM fails to start the underlying process, it needs to dump the
	 * file contents before continuing.
	 */
	@Override
	public synchronized void run() {

		try {
			getLogFilesMap(logFileMap);
			for (final String key : logFileMap.keySet()) {
				if (logFileMap.get(key).wasModified()) {
					final String lines = logFileMap.get(key).readLines();
					final String[] seporatedLines = lineSplitPattern.split(lines);
					for (final String line : seporatedLines) {
						handler.handleLine(key, line);
						// logger.info(line);
					}
				}
			}

		} catch (final Exception e) {
			logger.warning("Exception thrown: " + e.getMessage());
		}

	}

	/**
	 * Scans the folder for new files added to the logs folder. If a new file that is not contained in the map is found,
	 * it is added to the map. If a file no longer exists, it will be taken out of the map.
	 *
	 * @param logFileList
	 */
	private void getLogFilesMap(final Map<String, RollingFileReader> logFileMap) {

		final File folder = new File(logsDirectory);
		// Get list of files according to regex.
		final File[] files = folder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return java.util.regex.Pattern.matches(regex, name);
			}
		});

		// add newly created files if exist.
		for (final File file : files) {
			if (!logFileMap.containsKey(file.getName())) {
				logFileMap.put(file.getName(), new RollingFileReader(file));
			}
		}

		// remove files that no longer exist.
		final Iterator<RollingFileReader> iterator = logFileMap.values().iterator();
		while (iterator.hasNext()) {
			final RollingFileReader next = iterator.next();
			if (!next.exists()) {
				iterator.remove();
			}
		}
	}
}
