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
package org.openspaces.usm;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.cloudifysource.usm.tail.RollingFileAppenderTailer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.springframework.util.FileSystemUtils;


@RunWith(BlockJUnit4ClassRunner.class)
public class USMRollingFileAppenderTailerTest {

	private static final int RFAT_SAMPLING_RATE_MILLISECOND = 100;

	private static final int NUMBER_OF_LINES_TO_LOG = 30;

	private static final int EXPECTED_NUMBER_OF_FILE_PARTS = 2;

	// This is the pattern that Log4j will use when logging:
	// %d{ISO8601} %5p %c{1}:%L - %m%n
	private static final String LOG_PATTERN = "%d{ISO8601} %5p [%l] - %m%n";
	private static final int LOG_IO_BUFFER_SIZE_BYTES = 1024;
	private static final String LOG_FILENAME = "my.log";
	private static final int MAX_LOG_BACKUP_FILES = 20;
	private static final String MAX_LOG_FILE_SIZE = "3KB";
	private static final double MAX_LOG_FILE_SIZE_DOUBLE = 3;
	
	// We use the root logger for everything so we can capture all of the output
	// from shared libraries that use Log4j too
	public static final Logger logger = Logger.getRootLogger();
	
	private String logsDirectory = new File(System.getProperty("java.io.tmpdir"), "testRollingFileAppenderTailer").getAbsolutePath();
	private String regex = "my.*\\.log";
	private RollingFileAppender rfp;
	
	@Before
	public void before() throws Exception {

		// Where the logs will go.
		final File logDir = new File(logsDirectory);

		FileSystemUtils.deleteRecursively(logDir);
		logDir.mkdirs();

		final File logFile = new File(logDir, String.format("%s%s", 
		        System.getProperty("file.separator"), LOG_FILENAME));

		// Create a new pattern layout with our requested log pattern.
		final PatternLayout pl = new PatternLayout(LOG_PATTERN);

		rfp = new RollingFileAppender(pl, logFile.getCanonicalPath(), true);

		// We want the logger to flush its output to the log file
		// stream immediately; if you don't have this set, then
		// Log4j will buffer the log file output.
		rfp.setImmediateFlush(true);
		rfp.setBufferedIO(false);
		rfp.setBufferSize(LOG_IO_BUFFER_SIZE_BYTES);

		// Set the Max number of files and max size of each log
		// file to keep around.
		rfp.setMaxBackupIndex(MAX_LOG_BACKUP_FILES);
		rfp.setMaxFileSize(MAX_LOG_FILE_SIZE);

		// Set the default level of this logger.
		logger.setLevel(Level.INFO);
		// This logger will use the rolling appender.
		logger.addAppender(rfp);
		
		logger.getAllAppenders();
		
		
	    logger.info("Log directory: " + logDir.getAbsolutePath());
		
	}
	
	@Test
	public void rollingFileAppenderTailer() throws InterruptedException {
		
		//added handler to monitor tailing of the new file.
		StringHandler stringHandler = addHandlerToJavaUtilsLogger();
		//Start tailing the logs folder. 
		RollingFileAppenderTailer.start(logsDirectory, this.regex, RFAT_SAMPLING_RATE_MILLISECOND);
		
		//Start the log4j logging.
		startLogging();
		
		assertFileRolling();
		assertJavaUtilsTailedLogging(stringHandler.getLoggedMessages());
	}

	@After
	public void after() {
	    if (rfp != null) {
	        rfp.close();
	    }
        FileSystemUtils.deleteRecursively(new File(logsDirectory));
    }
	
	private void assertFileRolling() {
		
		File folder = new File(logsDirectory);
		
		//Get list of files according to regex.
		File[] files = folder.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name){
				return java.util.regex.Pattern.matches("my.*\\.log.*", name);
			}
		});
		//Check file rolling occurred.
		Assert.assertTrue(files.length == EXPECTED_NUMBER_OF_FILE_PARTS);
		
		//Test file sizes
		for (File file : files) {
			long fileSize = file.length();
			double fileSizeInKB = (double)fileSize/LOG_IO_BUFFER_SIZE_BYTES;
			//assert no file is over the size limit defined in the RollingFileAppender.
			Assert.assertTrue(fileSizeInKB < MAX_LOG_FILE_SIZE_DOUBLE + 1);
		}
		
	}

	private void assertJavaUtilsTailedLogging(String loggedMessages) {
		int counter = 0;
		//use "[\\r\\n]+ regular expression to filter out new lines without empty lines.
		String seporatedLines[] = loggedMessages.split("[\\r\\n]+");
		counter += seporatedLines.length;
		
		//worst case is that the tailer will miss the last line of a file before it's being rolled,
		//So we expect for the number of lines to be [Total number of lines - number of files] in the worst case. 
		Assert.assertTrue(NUMBER_OF_LINES_TO_LOG - counter < EXPECTED_NUMBER_OF_FILE_PARTS);
		
	}

	/**
	 * write some information to the log. this writing should produce 7 log files
	 * all of size that is not bigger then 128KB.
	 */
	private void startLogging() {
		for(int i = 0; i < NUMBER_OF_LINES_TO_LOG; i++){
			logger.info( "Epoch is " + new Date().getTime() + "  " + i);
			try {
				Thread.sleep(RFAT_SAMPLING_RATE_MILLISECOND * 2);
			} catch (InterruptedException e) {
			    // Do nothing
			}
		}
		
	}

	//Add a logging handler.
	private StringHandler addHandlerToJavaUtilsLogger() {
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger("");
		StringHandler sh = new StringHandler(); 
		logger.addHandler(sh);
		Handler[] handlers = logger.getHandlers();
		for (Handler handler : handlers) {
			if (handler instanceof ConsoleHandler){
				logger.removeHandler(handler);
				break;
			}
		}
		return sh;
	}
}
