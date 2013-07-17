package org.cloudifysource.utilitydomain.openspaces;

import org.cloudifysource.dsl.internal.CloudDependentConfigHolder;
import org.openspaces.maven.support.OutputVersion;

import com.j_spaces.kernel.PlatformVersion;

/**
 * 
 * @author adaml
 *
 */
public class OpenspacesDomainUtils {
	
	public static CloudDependentConfigHolder getCloudDependentConfig() {
		final CloudDependentConfigHolder dependentConfig = new CloudDependentConfigHolder();
		
		final String cloudifyUrlAccordingToPlatformVersion = createCloudifyUrlAccordingToPlatformVersion();
		dependentConfig.setDownloadUrl(cloudifyUrlAccordingToPlatformVersion);
		dependentConfig.setDefaultLusPort(OpenspacesConstants.DEFAULT_LUS_PORT);
		return dependentConfig;
	}
	
	private static String createCloudifyUrlAccordingToPlatformVersion() {
		String cloudifyUrlPattern;
		String productUri;
		String editionUrlVariable;

		if (PlatformVersion.getEdition().equalsIgnoreCase(PlatformVersion.EDITION_CLOUDIFY)) {
			productUri = "org/cloudifysource";
			editionUrlVariable = "cloudify";
			cloudifyUrlPattern = "http://repository.cloudifysource.org/"
					+ "%s/" + OutputVersion.computeCloudifyVersion() + "/gigaspaces-%s-"
					+ PlatformVersion.getVersion() + "-" + PlatformVersion.getMilestone()
					+ "-b" + PlatformVersion.getBuildNumber();
			return String.format(cloudifyUrlPattern, productUri, editionUrlVariable);
		} else {
			productUri = "com/gigaspaces/xap";
			editionUrlVariable = "xap-premium";
			cloudifyUrlPattern = "http://repository.cloudifysource.org/"
					+ "%s/" + OutputVersion.computeXapVersion() + "/gigaspaces-%s-"
					+ PlatformVersion.getVersion() + "-" + PlatformVersion.getMilestone()
					+ "-b" + PlatformVersion.getBuildNumber();
			return String.format(cloudifyUrlPattern, productUri, editionUrlVariable);
		}
	}
}
