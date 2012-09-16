package org.cloudifysource.esc.driver.provisioning.events;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.openspaces.grid.gsm.machines.plugins.events.MachineStartRequestedEvent;

import com.gigaspaces.internal.io.IOUtils;

public class MachineStartRequestedCloudifyEvent extends MachineStartRequestedEvent {

	private static final long serialVersionUID = 1L;
	
	private String templateName;
	private String locationId;
	
	/**
     * Deserialization cotr
     */
    public MachineStartRequestedCloudifyEvent() {
    }
    
	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}
	
	public String getLocationId() {
		return locationId;
	}
		
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        IOUtils.writeString(out, templateName);
        IOUtils.writeString(out, locationId);
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        templateName = IOUtils.readString(in);
        locationId = IOUtils.readString(in);
    }

}
