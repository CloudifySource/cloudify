package org.cloudifysource.esc.driver.provisioning.events;

import com.gigaspaces.internal.io.IOUtils;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStartRequestedEvent;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

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
    
    @Override
    public String getDecisionDescription() {
        StringBuilder desc = new StringBuilder(super.getDecisionDescription());
        if (templateName != null ) { 
        	desc.append(" using template ").append(templateName);
        }
        if (locationId != null) {
        	desc.append(" in location ").append(locationId);
        }
        return desc.toString();
    }
    
}
