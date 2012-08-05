package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlType(name = "OSVirtualHardDisk")
public class OSVirtualHardDisk {
	
	private String mediaLink;	
	private String sourceImageName;

	@XmlElement(name = "MediaLink")
	public String getMediaLink() {
		return mediaLink;
	}

	public void setMediaLink(final String mediaLink) {
		this.mediaLink = mediaLink;
	}

	@XmlElement(name = "SourceImageName")
	public String getSourceImageName() {
		return sourceImageName;
	}

	public void setSourceImageName(final String sourceImageName) {
		this.sourceImageName = sourceImageName;
	}
}
