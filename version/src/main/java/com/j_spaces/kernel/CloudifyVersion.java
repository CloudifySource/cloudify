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
package com.j_spaces.kernel;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

public class CloudifyVersion implements ProductVersion {
	private String EDITION = "Cloudify";
	// !!!IMPORTANT, read below
	// Must be of this format otherwise PlatformLogicalVersion will fail
	// parsing!!!
	private String VERSION = "2.7.1";
	private String MILESTONE = "m2";
	private String BUILD_TYPE = "regular";
	private String V_NUM = VERSION + '-' + EDITION + '-' + MILESTONE;
	private String V_LICENSE_NUM = "2.2" + EDITION;
	// !!!IMPORTANT, read below
	// Must be of either "int-int-string", "int-int" or "int" format otherwise
	// PlatformLogicalVersion will fail parsing!!!
	private final String BUILD_NUM = "6201-77";
	private final String V_NAME = "GigaSpaces";
	private final String PRODUCT_HELP_URL = "http://www.cloudifysource.org/guide";
    private final String BUILD_TIMESTAMP = "6201-77";

	/** default constructor for Class.forName() - see com.j_spaces.kernel.PlatformVersion */
	public CloudifyVersion() {
	}
	
	@Override
	public String getOfficialVersion() {
		return V_NAME + " " + getShortOfficialVersion() + " (build "
		+ BUILD_NUM + ", timestamp " + BUILD_TIMESTAMP + ")";
	}

	@Override
	public String getShortOfficialVersion() {
		String edition = EDITION;
		final String XAP_Prefix = "XAP";
		if (EDITION.startsWith("XAP")) {
			edition = XAP_Prefix + " " + EDITION.substring(XAP_Prefix.length());
		}

		return edition + " " + VERSION + " " + MILESTONE.toUpperCase();
	}

	@Override
	public String getVersionAndBuild() {
		return VERSION + "." + BUILD_NUM;
	}

	@Override
	public void createBuildNumberPropertyFile() {
		FileOutputStream fileOut = null;
		PrintStream ps = null;

		try {
			fileOut = new FileOutputStream("build.properties", true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		ps = new PrintStream(fileOut);

		ps.println("buildnumber=" + BUILD_NUM);
		ps.println("tag=" + getTag());
		ps.println("versionnumber=" + V_NUM);
		ps.println("milestone=" + MILESTONE);
		ps.println("productversion=" + VERSION);

		ps.close();
	}

	@Override
	public String getTag() {
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
		String date = dateFormat.format(new Date());

		String tag = "build" + BUILD_NUM + "_" + date;

		return tag;
	}
	
	@Override
	public String getEdition() {
		return EDITION;
	}

	@Override
	public String getVersion() {
		return VERSION;
	}
	
	@Override
	public String getLicenseVersion() {
		return V_LICENSE_NUM;
	}

	@Override
	public String getBuildNumber() {
		return BUILD_NUM;
	}

	@Override
	public String getVersionNumber() {
		return V_NUM;
	}

	@Override
	public String getMilestone() {
		return MILESTONE;
	}

	@Override
	public String getBuildType() {
		return BUILD_TYPE;
	}
	
	@Override
	public String getProductHelpUrl() {
		return PRODUCT_HELP_URL;
	}

    @Override
    public String getBuildTimestamp() {
        return BUILD_TIMESTAMP;
    }
}
