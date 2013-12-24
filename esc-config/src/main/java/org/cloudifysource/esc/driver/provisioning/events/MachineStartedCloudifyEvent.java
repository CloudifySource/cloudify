package org.cloudifysource.esc.driver.provisioning.events;

import com.gigaspaces.internal.io.IOUtils;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStartedEvent;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class MachineStartedCloudifyEvent extends MachineStartedEvent {

	private static final long serialVersionUID = 1L;
	
	private MachineDetails machineDetails;
	
	/**
     * Deserialization cotr
     */
    public MachineStartedCloudifyEvent() {
    }
 
	public MachineDetails getMachineDetails() {
		return machineDetails;
	}

	public void setMachineDetails(MachineDetails machineDetails) {
		this.machineDetails = machineDetails;
	}
	
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        IOUtils.writeObject(out, machineDetails);
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        machineDetails = IOUtils.readObject(in);
    }
}
