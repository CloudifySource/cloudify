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
	private String VERSION = "2.0.0";
	private String MILESTONE = "ga";
	private String BUILD_TYPE = "regular";
	private String V_NUM = VERSION + '-' + EDITION + '-' + MILESTONE;
	private String V_LICENSE_NUM = "2.0" + EDITION;
	// !!!IMPORTANT, read below
	// Must be of either "int-int-string", "int-int" or "int" format otherwise
	// PlatformLogicalVersion will fail parsing!!!
	private final String BUILD_NUM = "998-2";
	private final String V_NAME = "GigaSpaces";
	private final String PRODUCT_HELP_URL = "cloudify.product.help.url=http://www.gigaspaces.com/wiki/display/CLOUD";

	public CloudifyVersion() {
		throw new RuntimeException("not cloudify");
	}
	
	@Override
	public String getOfficialVersion() {
		return V_NAME + " " + getShortOfficialVersion() + " (build "
				+ BUILD_NUM + ")";
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
}