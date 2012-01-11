package org.cloudifysource.usm.liveness;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
/**
 * An implementation for TailerListener. The FileListener will check each
 * new line written to the file and raise a flag if the desired regular expression
 * was found. 
 * 
 * @author adaml
 *
 */
public class FileTailerListener implements TailerListener {

	private volatile boolean isProcessUp = false;
	private volatile Pattern pattern; 
	
	private static final Logger logger = Logger.getLogger(FileTailerListener.class.getName());

	public FileTailerListener(String regex){
		pattern = Pattern.compile(regex);
	}
	
	/**
	 * handle is being called each predefined time period.
	 */
	public void handle(String line) {
		Matcher matcher = pattern.matcher(line);
		if (matcher.find()){
			isProcessUp = true;
		}
	}
	
	public boolean isProcessUp(){
		return isProcessUp;
	}

	public void handle(Exception e) {
		logger.warning("The file listener has handled an exception: " + e.getMessage());
	}
	public void init(Tailer tailer) {
		logger.info("A new tailer object was constructed");
	}
	public void fileNotFound() {
		logger.info("The tailed file is not found");
	}
	public void fileRotated() {
		logger.info("The filename was changed");
	}
}