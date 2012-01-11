package org.cloudifysource.usm.liveness;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.io.input.Tailer;
import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.events.AbstractUSMEventListener;

import com.gigaspaces.cloudify.dsl.Plugin;
import com.gigaspaces.cloudify.dsl.context.ServiceContext;
/**
 * FileLivenessDetector class is responsible for verifying that the process has finished loading
 * by checking whether the desired regex was found in the process's output log.
 * The path to the process's log file is defined in the groovy configuration file.
 * 
 * Using the FileLivenessDetector requires adding a plugin to the DSL file as following:
 * 	plugins ([
 *		plugin {
 *			name "fileLiveness"
 *			className "com.gigaspaces.cloudify.usm.liveness.FileLivenessDetector"
 *			config ([
 *						"FilePath" : System.getProperty("java.io.tmpdir") + "/groovyLog.log",
 *						"TimeoutInSeconds" : 30,
 *						"regularExpression" : "Hello_World"
 *					])
 *		},
 *		plugin {...
 * @author adaml
 *
 */
public class FileLivenessDetector extends AbstractUSMEventListener implements LivenessDetector, Plugin{

	public static final String TIMEOUT_IN_SECONDS_KEY = "TimeoutInSeconds";
	public static final String REGULAR_EXPRESSION_KEY = "regularExpression";
	public static final String FILE_PATH_KEY = "FilePath";

	private static final Logger logger = Logger.getLogger(FileLivenessDetector.class.getName());

	private String filePath = "";
	private String regex = "";
	private int timeoutInSeconds = 60;

	private static final int TIMEOUT_BETWEEN_FILE_QUERYING = 1000;
	private String serviceDirectory;


	@Override
	public void setConfig(Map<String, Object> config) {
		if (config.get(FILE_PATH_KEY) != null){
			this.filePath = ((String) config.get(FILE_PATH_KEY));
		}

		if (config.get(TIMEOUT_IN_SECONDS_KEY) != null){
			this.timeoutInSeconds = (Integer) config.get(TIMEOUT_IN_SECONDS_KEY);
		}
		if (config.get(REGULAR_EXPRESSION_KEY) != null){
			this.regex = (String) config.get(REGULAR_EXPRESSION_KEY);
		}
	}

	/**
	 * isProcessAlive will sample the file defined in the groovy configuration file
	 * every second for the specified timeout period looking for a regex in the log that confirms the process 
	 * has loaded successfully and return true if the regex was found.
	 * @throws USMException 
	 * 
	 */
	@Override
	public boolean isProcessAlive() throws USMException {
		if (this.regex == "" || this.filePath == ""){
			throw new USMException("When using the FileLivnessDetector, both the file path and regex should be defined.");
		}
		File file = new File(this.filePath);
		if (!file.isAbsolute()){
			file = new File(serviceDirectory, this.filePath);
		}
		FileTailerListener listener = new FileTailerListener(this.regex);
		Tailer tailer = null;
		try{
			long startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() < startTime + timeoutInSeconds * 1000) {
				if (file.exists()){
					if (tailer == null){
						tailer = Tailer.create(file, listener, TIMEOUT_BETWEEN_FILE_QUERYING, false);
					}
					if (listener.isProcessUp()){
						logger.info("The regular expression " + this.regex + " was found in the process log");
						return true;
					}
				}
				try {
					Thread.sleep(TIMEOUT_BETWEEN_FILE_QUERYING);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}finally{
			if (tailer != null){
				tailer.stop();
			}
		}
		logger.info("The regular expression " + this.regex + " was NOT found in the process log");
		return false;

	}

	@Override
	public void setServiceContext(ServiceContext context) {
		serviceDirectory = context.getServiceDirectory();
	}
}
