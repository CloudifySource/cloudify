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
	@Override
	public void handle(String line) {
		Matcher matcher = pattern.matcher(line);
		if (matcher.find()){
			isProcessUp = true;
		}
	}
	
	public boolean isProcessUp(){
		return isProcessUp;
	}

	@Override
	public void handle(Exception e) {
		logger.warning("The file listener has handled an exception: " + e.getMessage());
	}
	@Override
	public void init(Tailer tailer) {
		logger.info("A new tailer object was constructed");
	}
	@Override
	public void fileNotFound() {
		logger.info("The tailed file is not found");
	}
	@Override
	public void fileRotated() {
		logger.info("The filename was changed");
	}
}